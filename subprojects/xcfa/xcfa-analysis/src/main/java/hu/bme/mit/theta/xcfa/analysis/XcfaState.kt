/*
 *  Copyright 2022 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hu.bme.mit.theta.xcfa.analysis

import hu.bme.mit.theta.analysis.expr.ExprState
import hu.bme.mit.theta.core.decl.VarDecl
import hu.bme.mit.theta.core.stmt.AssignStmt
import hu.bme.mit.theta.core.stmt.AssumeStmt
import hu.bme.mit.theta.core.stmt.Stmts.Assign
import hu.bme.mit.theta.core.stmt.Stmts.Assume
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.core.type.abstracttype.ModExpr
import hu.bme.mit.theta.core.type.anytype.RefExpr
import hu.bme.mit.theta.core.type.booltype.BoolType
import hu.bme.mit.theta.core.utils.TypeUtils.cast
import hu.bme.mit.theta.frontend.transformation.grammar.expression.Reference
import hu.bme.mit.theta.frontend.transformation.model.types.complex.CComplexType
import hu.bme.mit.theta.frontend.transformation.model.types.complex.compound.CPointer
import hu.bme.mit.theta.xcfa.exchangeAliases
import hu.bme.mit.theta.xcfa.getFlatLabels
import hu.bme.mit.theta.xcfa.model.*
import java.util.*

data class XcfaState<S : ExprState> @JvmOverloads constructor(
        val xcfa: XCFA?, // TODO: remove this
        val processes: Map<Int, XcfaProcessState>,
        val sGlobal: S,
        val mutexes: Map<String, Int> = processes.keys.associateBy { "$it" },
        val threadLookup: Map<VarDecl<*>, Int> = emptyMap(),
        val bottom: Boolean = false,
        val pointerAliases: Map<VarDecl<*>, VarDecl<*>> = emptyMap()
): ExprState {
    override fun isBottom(): Boolean {
        return bottom || sGlobal.isBottom
    }

    override fun toExpr(): Expr<BoolType> {
        return sGlobal.toExpr()
    }

    fun apply(a: XcfaAction) : Pair<XcfaState<S>, XcfaAction>{
        val changes: MutableList<(XcfaState<S>) -> XcfaState<S>> = ArrayList()

        val processState = processes[a.pid]
        checkNotNull(processState)
        check(processState.locs.peek() == a.source)
        val newProcesses: MutableMap<Int, XcfaProcessState> = LinkedHashMap(processes)
        newProcesses[a.pid] = checkNotNull(processes[a.pid]?.withNewLoc(a.target))
        if(processes != newProcesses) {
            changes.add { state -> state.withProcesses(newProcesses) }
        }

        val newLabels: MutableList<XcfaLabel> = ArrayList()
        val newAliases: MutableMap<VarDecl<*>, VarDecl<*>> = LinkedHashMap(pointerAliases)
        a.edge.getFlatLabels().forEach {
            when(it) {
                is FenceLabel -> it.labels.forEach { label ->
                    when(label) {
                        "ATOMIC_BEGIN" -> changes.add { it.enterMutex("", a.pid) }
                        "ATOMIC_END" -> changes.add { it.exitMutex("", a.pid) }
                        in Regex("mutex_lock\\((.*)\\)") -> changes.add { state -> state.enterMutex( label.substring("mutex_lock".length + 1, label.length-1), a.pid)}
                        in Regex("mutex_unlock\\((.*)\\)") -> changes.add { state -> state.exitMutex( label.substring("mutex_unlock".length + 1, label.length-1), a.pid )}
                    }
                }
                is InvokeLabel -> error("Function invocations not yet supported")
                is JoinLabel -> {
                    changes.add { state -> state.enterMutex(it.pidVar.name, a.pid) }
                    changes.add { state -> state.exitMutex(it.pidVar.name, a.pid) }
                }
                is NondetLabel -> newLabels.add(it)
                NopLabel -> {}
                is ReadLabel -> error("Read/Write labels not yet supported")
                is SequenceLabel -> error("Sequence labels should have been already eliminated")
                is StartLabel ->  changes.add { state -> state.start(it) }.let { false }
                is StmtLabel -> newLabels.add(changeRefs(it, newAliases))
                is AssignDereferencedVariableLabel -> {
                    val alias = (it.lhs.exchangeAliases(newAliases) as RefExpr<*>).decl as VarDecl<*>
                    newLabels.add(changeRefs(StmtLabel(Assign(cast(alias, alias.type), cast(it.rhs, alias.type)), ChoiceType.NONE, it.metadata), newAliases))
                }
                is WriteLabel -> error("Read/Write labels not yet supported")
            }
        }

        if(a.target.final) {
            if(checkNotNull(newProcesses[a.pid]).locs.size == 1) {
                changes.add { state -> state.endProcess(a.pid) }
            }
        }

        return Pair(changes.fold(this) { current, change -> change(current) }, a.withLabel(SequenceLabel(newLabels)))
    }

    private fun changeRefs(label: StmtLabel, mutAliases: MutableMap<VarDecl<*>, VarDecl<*>>): XcfaLabel {
        return when(label.stmt) {
            is AssumeStmt -> label.copy(stmt = Assume((label.stmt as AssumeStmt).cond.exchangeAliases(mutAliases) as Expr<BoolType>))
            is AssignStmt<*> -> {
                val expr = (label.stmt as AssignStmt<*>).expr
                val varDecl = (label.stmt as AssignStmt<*>).varDecl
                return if(expr is Reference<*, *>) {
                    mutAliases[varDecl] = (expr.op as RefExpr<*>).decl as VarDecl<*>
                    NopLabel
                } else if (CComplexType.getType(varDecl.ref) is CPointer) {
                    var expr = expr
                    if(expr is ModExpr<*>) expr = expr.leftOp // TODO: is this sound?
                    check(expr is RefExpr) { "Cannot perform pointer arithmetic." }
                    mutAliases[varDecl] = mutAliases[expr.decl as VarDecl<*>] ?: error("Uninitialized pointer implicitly dereferenced.")
                    NopLabel
                } else {
                    label.copy(stmt = Assign(
                            cast(varDecl, varDecl.type),
                            cast(expr.exchangeAliases(mutAliases), varDecl.type)))
                }
            }
            else -> {label}
        }
    }

    private fun start(startLabel: StartLabel): XcfaState<S> {
        val newProcesses: MutableMap<Int, XcfaProcessState> = LinkedHashMap(processes)

        val procedure = checkNotNull(xcfa?.procedures?.find { it.name == startLabel.name })
        val pid = newProcesses.size
        newProcesses[pid] = XcfaProcessState(LinkedList(listOf(procedure.initLoc)))
        val newMutexes = LinkedHashMap(mutexes)
        newMutexes["$pid"] = pid

        return copy(processes=newProcesses, mutexes=newMutexes)
    }

    private fun endProcess(pid: Int): XcfaState<S> {
        val newProcesses: MutableMap<Int, XcfaProcessState> = LinkedHashMap(processes)
        newProcesses.remove(pid)
        val newMutexes = LinkedHashMap(mutexes)
        newMutexes.remove("$pid")
        return copy(processes=newProcesses)
    }

    fun enterMutex(key: String, pid: Int): XcfaState<S> {
        if(mutexes.keys.any { Regex(it).matches(key) }) return copy(bottom = true)

        val newMutexes = LinkedHashMap(mutexes)
        newMutexes[key] = pid
        return copy(mutexes = newMutexes)
    }

    fun exitMutex(key: String, pid: Int): XcfaState<S> {
        val newMutexes = LinkedHashMap(mutexes)
        newMutexes.remove(key, pid)
        return copy(mutexes = newMutexes)
    }


    private fun withProcesses(nP: Map<Int, XcfaProcessState>): XcfaState<S> {
        return copy(processes=nP)
    }

    fun withState(s: S): XcfaState<S> {
        return copy(sGlobal=s)
    }

    override fun toString(): String {
        return "$processes {$sGlobal}"
    }
}

data class XcfaProcessState(
        val locs: LinkedList<XcfaLocation>
) {
    fun withNewLoc(l: XcfaLocation) : XcfaProcessState {
        val deque: LinkedList<XcfaLocation> = LinkedList(locs)
        deque.pop()
        deque.push(l)
        return XcfaProcessState(deque)
    }

    override fun toString(): String = when(locs.size) {
        0 -> ""
        1 -> locs.peek()!!.toString()
        else -> "${locs.peek()!!} [${locs.size}]"
    }
}

operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)
