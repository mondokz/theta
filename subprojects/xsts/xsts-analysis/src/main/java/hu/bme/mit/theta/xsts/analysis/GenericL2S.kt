package hu.bme.mit.theta.xsts.analysis

import hu.bme.mit.theta.analysis.Action
import hu.bme.mit.theta.analysis.LTS
import hu.bme.mit.theta.analysis.State
import hu.bme.mit.theta.core.decl.Decl
import hu.bme.mit.theta.core.decl.Decls
import hu.bme.mit.theta.core.decl.VarDecl
import hu.bme.mit.theta.core.stmt.AssignStmt
import hu.bme.mit.theta.core.stmt.Stmt
import hu.bme.mit.theta.core.stmt.Stmts
import hu.bme.mit.theta.core.stmt.Stmts.*
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.core.type.Type
import hu.bme.mit.theta.core.type.booltype.BoolExprs
import hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool
import hu.bme.mit.theta.core.type.booltype.BoolExprs.True
import hu.bme.mit.theta.core.type.booltype.BoolType

// LTL ellenőrzés különböző eszközökkel (NuXMV, SPIN)
// OR
// LTL+Model -> Büchi Automata + Model -> Büchi Automata X Model -> loop checker
// SPOT, LTL2BA, OWL; Warning: they like penguins and hate windows (WSL: windows subsystem for linux)

/*
data class L2SInputs<A: Action>(
    val allVars: Collection<VarDecl<*>>,
    val structuralNext: (A) -> Stmt,
    val prop: Expr<BoolType>,
    val init: Expr<BoolType>,
)
interface AdaptModelToL2S<M, A: Action> {
    fun adapt(M model): L2SInputs<A>
}
*/
class GenericL2S<S: State, A: Action>(
    val lts: LTS<S, A>,
    val allVars: Collection<VarDecl<*>>,
    val structuralNext: (A) -> Stmt,
    val prop: Expr<BoolType>,
    val init: Expr<BoolType>,
    val extend: (A, Stmt) -> A // ActionExtender<A>; XstsActionExtender: ActionExtender<XstsAction>; xstsActionExtender::extend
): LTS<S, A> {

    val savedVars = allVars.associate {
        it to Decls.Var("__${it.name}_saved", it.type)
    }
    val savedFlag = Decls.Var("__saved", Bool())
    private val subsave = savedVars.map { Assign(it.value, it.key.ref) }
    val saveStmt = SequenceStmt(listOf(savedVars.map { Assign(it.value, it.key.ref) }+Assign(savedFlag, True())))

    val newProp: Expr<BoolType> = TODO("transform prop")
    val newInit: Expr<BoolType> = TODO("transform init")

    override fun getEnabledActionsFor(state: S) =
        lts.getEnabledActionsFor(state).flatMap {
            val egyik = extend(it, SequenceStmt(listOf(saveStmt, structuralNext)))
            val masik = extend(it, structuralNext)
            listOf(egyik, masik)
        }


}