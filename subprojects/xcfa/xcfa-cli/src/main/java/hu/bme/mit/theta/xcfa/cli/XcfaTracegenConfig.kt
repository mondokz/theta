package hu.bme.mit.theta.xcfa.cli;

import hu.bme.mit.theta.analysis.Action
import hu.bme.mit.theta.analysis.Prec
import hu.bme.mit.theta.analysis.State
import hu.bme.mit.theta.analysis.Trace
import hu.bme.mit.theta.analysis.algorithm.SafetyResult
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor
import hu.bme.mit.theta.analysis.algorithm.tracegen.TraceGenChecker
import hu.bme.mit.theta.analysis.expr.ExprAction
import hu.bme.mit.theta.analysis.expr.ExprState
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.solver.SolverFactory
import hu.bme.mit.theta.solver.SolverManager
import hu.bme.mit.theta.solver.validator.SolverValidatorWrapperFactory
import hu.bme.mit.theta.solver.z3.Z3SolverManager
import hu.bme.mit.theta.xcfa.model.XCFA
import java.lang.RuntimeException
import kotlin.Suppress;

class XcfaTracegenConfig {
    var traces: MutableList<Trace<out State, out Action>> = arrayListOf()
    private val domain: Domain = Domain.EXPL

    @Suppress("UNCHECKED_CAST")
    private fun getTracegenChecker(xcfa: XCFA, logger: Logger): TraceGenChecker<ExprState, ExprAction, Prec>? {
        registerZ3(logger)

        val abstractionSolver = "Z3"
        val maxEnum: Int = 0
        val validateAbstractionSolver = false
        val search: Search = Search.DFS
        val refinement: Refinement = Refinement.SEQ_ITP // don't care

        val abstractionSolverFactory: SolverFactory = getSolver(abstractionSolver, validateAbstractionSolver)

        val abstractor: Abstractor<ExprState, ExprAction, Prec> = domain.abstractor(
                xcfa,
                abstractionSolverFactory.createSolver(),
                maxEnum,
                search.getComp(xcfa),
                refinement.stopCriterion,
                logger
        ) as Abstractor<ExprState, ExprAction, Prec>

        return TraceGenChecker.create(logger, abstractor)
    }
    fun check(xcfa: XCFA, logger: Logger): SafetyResult<ExprState, ExprAction> {
        val initPrec = InitPrec.ALLVARS
        val checker = getTracegenChecker(xcfa, logger)
        val result = checker?.check(domain.initPrec(xcfa, initPrec)) ?: throw RuntimeException("TracegenChecker is null")
        traces.clear()
        traces.addAll(checker.traces)
        return result
    }

    private fun getSolver(name: String, validate: Boolean) = if (validate) {
        SolverValidatorWrapperFactory.create(name)
    } else {
        SolverManager.resolveSolverFactory(name)
    }

    private fun registerZ3(logger: Logger) {
        SolverManager.closeAll()
        SolverManager.registerSolverManager(Z3SolverManager.create())
    }
}
