/*
 *  Copyright 2023 Budapest University of Technology and Economics
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

package hu.bme.mit.theta.analysis.algorithm.mcm.analysis

import hu.bme.mit.theta.analysis.*
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder
import hu.bme.mit.theta.analysis.algorithm.ArgNode
import hu.bme.mit.theta.analysis.algorithm.cegar.BasicAbstractor
import hu.bme.mit.theta.analysis.algorithm.cegar.abstractor.StopCriterion
import hu.bme.mit.theta.analysis.algorithm.cegar.abstractor.StopCriterions
import hu.bme.mit.theta.analysis.algorithm.mcm.interpreter.MemoryEventProvider
import hu.bme.mit.theta.analysis.algorithm.mcm.mcm.MemoryEvent
import hu.bme.mit.theta.analysis.waitlist.FifoWaitlist
import hu.bme.mit.theta.analysis.waitlist.Waitlist
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.core.decl.VarDecl
import java.util.function.Predicate

class AbstractExecutionGraph<S: State, A: Action, P: Prec>(
        val lts: LTS<S, A>,
        val partialOrd: PartialOrd<S>,
        val initFunc: InitFunc<S, P>,
        val transFunc: TransFunc<S, A, P>,
        val target: Predicate<S>,
        val memoryEventProvider: MemoryEventProvider<A, P>,
        val logger: Logger
    ) {

    fun check(initPrec: P) {
        /*
        ARGS <- {initial args} // needs a per-thread result from InitFunc
        for (ARG in ARGS) {
             expand(ARG) // using BasicAbstractor, needs: per-thread lts, partialOrd, initFunc, transFunc, target, stopCriterion, waitList
             findEvents(ARG)
        }
         */

        val execGraph = ExecutionGraph()
        val abstractor = createAbstractor(lts, partialOrd, initFunc, transFunc, target, StopCriterions.fullExploration(), FifoWaitlist.create())
        val arg = abstractor.createArg()
        abstractor.check(arg, initPrec)

        val edges = arg.nodes.map{it.inEdge}.filter{it.isPresent}.map{it.get()}.toList()
        val events = edges.map { Pair(memoryEventProvider[it.action, initPrec], it) }.toMap()
        val reads = LinkedHashMap<VarDecl<*>, MutableList<MemoryEvent.Read>>()
        events.keys.filter { it.type() == MemoryEvent.MemoryEventType.READ }.forEach {
            reads.putIfAbsent(it.asRead().`var`(), ArrayList())
            reads[it.asRead().`var`()]!!.add(it.asRead())
        }
        val writes = LinkedHashMap<VarDecl<*>, MutableList<MemoryEvent.Write>>()
        events.keys.filter { it.type() == MemoryEvent.MemoryEventType.WRITE }.forEach {
            writes.putIfAbsent(it.asWrite().`var`(), ArrayList())
            writes[it.asWrite().`var`()]!!.add(it.asWrite())
        }

        for ((globalVar, writeList) in writes) {
            for(read in reads[globalVar] ?: emptyList()) {
                val argEdge = checkNotNull(events[read])
                arg.prune(argEdge.target)
                for (write in writeList) {
                    // add new branches
                }
            }
        }

        // check feasibility, interpolate -> new prec
    }

    private fun createAbstractor(
            lts: LTS<S, A>,
            partialOrd: PartialOrd<S>,
            initFunc: InitFunc<S,P>,
            transFunc: TransFunc<S, A, P>,
            target: Predicate<S>,
            stopCriterion: StopCriterion<S, A>,
            waitlist: Waitlist<ArgNode<S, A>>
    ): BasicAbstractor<S, A, P> {
        val analysis = object:Analysis<S,A,P> {
            override fun getPartialOrd(): PartialOrd<S> = partialOrd
            override fun getInitFunc(): InitFunc<S, P> = initFunc
            override fun getTransFunc(): TransFunc<S, A, P> = transFunc
        }
        val argBuilder = ArgBuilder.create(lts, analysis, target)
        val abstractorBuilder = BasicAbstractor.builder(argBuilder)
        return abstractorBuilder.logger(logger).stopCriterion(stopCriterion).waitlist(waitlist).build()
    }

}