/*
 *  Copyright 2025 Budapest University of Technology and Economics
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

package analysis;

import hu.bme.mit.theta.analysis.*;
import hu.bme.mit.theta.analysis.algorithm.Proof;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.ImmutableValuation;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.abstracttype.EqExpr;
import hu.bme.mit.theta.core.type.booltype.AndExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.PathUtils;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;
import hu.bme.mit.theta.solver.UCSolver;
import hu.bme.mit.theta.solver.z3legacy.Z3LegacySolverFactory;
import hu.bme.mit.theta.sts.STS;
import hu.bme.mit.theta.sts.analysis.StsAction;
import hu.bme.mit.theta.sts.analysis.StsToMonolithicExprKt;
import hu.bme.mit.theta.sts.analysis.config.StsConfig;
import hu.bme.mit.theta.sts.analysis.config.StsConfigBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.False;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.*;

public class RLiveChecker<P extends Prec> implements SafetyChecker<Proof, Cex, P> {
    private final TempChecker<P, InvariantForRlive, Trace<Valuation, StsAction>> baseChecker;
    private final MonolithicExpr monolithicExpr;
    private Expr<BoolType> c;
    final boolean pruneEnabled;
    private final UCSolver ucSolver;
    private final Logger logger;
    private final Logger.Level level = Logger.Level.VERBOSE;

    public RLiveChecker(
            final MonolithicExpr monolithicExpr,
            final TempChecker<P, InvariantForRlive, Trace<Valuation, StsAction>> baseChecker,
            final boolean pruneEnabled) {
        this.pruneEnabled = pruneEnabled;
        this.monolithicExpr = Objects.requireNonNull(monolithicExpr);
        this.baseChecker = Objects.requireNonNull(baseChecker);
        this.c = False();
        this.ucSolver = Z3LegacySolverFactory.getInstance().createUCSolver();
        this.logger = new ConsoleLogger(Logger.Level.VERBOSE);
    }


    @Override
    public SafetyResult<Proof, Cex> check(final P input) {
        c = False();
        logger.write(level, "r-live started with: ");
        logger.write(level, pruneEnabled ? "pruning enabled \n" : "pruning disabled \n");
        while (true) {

            var sts = prepareExpressions(monolithicExpr.getTransExpr(), monolithicExpr.getPropExpr(), monolithicExpr.getInitExpr());
            SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> result = checkReachability(sts);

            if (result.isUnsafe()) {
                List<Trace<Valuation, StsAction>> traceList = new ArrayList<>();
                traceList.add(getTrace(result));
                Valuation s = extractReachedNotQState(result);
                Map<Valuation, Integer> visited = new HashMap<>();
                var cex = searchCex(s, visited, traceList);

                if (cex != null) {
                    List<ExplState> states = new ArrayList<>(cex.stream().flatMap(trace -> trace.getStates().stream()).map(ExplState::of).toList());
                    states.add(0, ExplState.of(ImmutableValuation.from(Collections.emptyMap())));
                    List<StsAction> actions = Collections.nCopies(states.size() - 1, StsAction.of(StsToMonolithicExprKt.fromMonolithicExpr(monolithicExpr)));
                    return SafetyResult.unsafe(Trace.of(states, actions), result.asUnsafe().getProof());
                }
            } else {
                logger.write(level,"CANT find new bad state from start");
                return SafetyResult.safe(result.asSafe().getProof());
            }

        }
    }

    private List<Trace<Valuation, StsAction>> searchCex(final Valuation s, Map<Valuation, Integer> visited, List<Trace<Valuation, StsAction>> traceList) {
        int stateId;
        if (visited.containsKey(s)) {
            logger.write(level,"REVISITED bad state #%d ", visited.get(s));
            return traceList;
        } else {
            stateId = visited.size() + 1;
            if (stateId == 1){
                logger.write(level,"NEW bad state found from INIT: #%d%n", visited.size() + 1);
                visited.put(s, visited.size() + 1);
            } else {
                logger.write(level,"NEW bad state found: #%d%n", visited.size() + 1);
                visited.put(s, visited.size() + 1);
            }

        }
        while (true) {

            if (pruneEnabled) {
                logger.write(level,"prune starting \n");
                var result = pruneDead(s);
                logger.write(level,"prune finished with result: %b \n", result);
                if (result) {
                    return null;
                }
            }

            var sts = prepareExpressions(monolithicExpr.getTransExpr(), monolithicExpr.getPropExpr(), s.toExpr());
            logger.write(level,"searching new bad state from #%d \n", stateId);
            SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> result = checkReachability(sts);
            logger.write(level,"search finished: ");


            if (result.isUnsafe()) {
                Valuation t = extractReachedNotQState(result);
                List<Trace<Valuation, StsAction>> newTraceList = new ArrayList<>(traceList);
                newTraceList.add(getTrace(result));
                var cex = searchCex(t, visited, newTraceList);
                if (cex != null) {
                    return cex;
                }
            } else {
                var invariant = result.getProof();
                c = Or(c, invariant.getInvariant());
                logger.write(level,"backtrack from shoal \n");
                return null;
            }
        }
    }

    private Trace<Valuation, StsAction> getTrace(SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> result) {
        List<Valuation> valuationList = new ArrayList<>();
        result.asUnsafe().getCex().getStates().forEach(val -> {

            Map<Decl<?>, LitExpr<?>> x = val.toMap().entrySet().stream()
                    .filter(entry -> !entry.getKey().getName().contains("_temp"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            valuationList.add(ImmutableValuation.from(x));
        });
        var actionList = result.asUnsafe().getCex().getActions().stream().map(x -> StsAction.of(StsToMonolithicExprKt.fromMonolithicExpr(monolithicExpr))).toList();
        return Trace.of(valuationList, actionList);
    }

    private boolean pruneDead(final Valuation s) {
        while (true) {
            var cPrime = ExprUtils.applyPrimes(c, VarIndexingFactory.indexing(1));
            var expr = And(s.toExpr(), monolithicExpr.getTransExpr(), Not(cPrime));
            ucSolver.push();
            ucSolver.track(PathUtils.unfold(expr, 0));
            var status = ucSolver.check(); // avoid duplicate solver calls
            if (status.isSat()) {
                var model = ucSolver.getModel();
                var l = PathUtils.unfold(PathUtils.extractValuation(model, 1).toExpr(), VarIndexingFactory.indexing(0));
                ucSolver.pop();
                ucSolver.push();
                var tUnfold = PathUtils.unfold(monolithicExpr.getTransExpr(), 0);
                ucSolver.track(tUnfold);
                var notCUnfold = PathUtils.unfold(Not(cPrime), 0);
                ucSolver.track(notCUnfold);
                if (l instanceof AndExpr land) {
                    for (Expr<BoolType> op : land.getOps()) {
                        ucSolver.track(op);
                    }
                } else {
                    ucSolver.track(l);
                }
                var status2 = ucSolver.check();
                if (status2.isUnsat()) {
                    var uc = new ArrayList<>(ucSolver.getUnsatCore());
                    uc.remove(tUnfold);
                    uc.remove(notCUnfold);
                    c = Or(c, PathUtils.foldin(And(uc), 0));
                    ucSolver.pop();
                    // continue to attempt more pruning
                } else {
                    ucSolver.pop();
                    return false;
                }
            } else if (status.isUnsat()) {
                ucSolver.pop();
                return true;
            }


        }
    }


    private STS prepareExpressions(Expr<BoolType> t, Expr<BoolType> q, Expr<BoolType> i) {
        var cPrime = ExprUtils.applyPrimes(c, VarIndexingFactory.indexing(1));
        var transExpr = And(t, And(Not(c), Not(cPrime)));
        var targetExpr = And(Not(q), And(t, Not(cPrime)));

        STS tempSts = new STS(i,transExpr,Not(targetExpr));
        Map<VarDecl<?>,VarDecl<?>> varMap = new HashMap<>();

        tempSts.getVars().forEach(var -> {
            VarDecl<?> tempVar = Decls.Var(var.getName() + "_temp", var.getType());
            varMap.put(var,tempVar);
        });

        // for each v : v = v_temp''
        Collection<? extends Expr<BoolType>> varEqTempVar = varMap.entrySet().stream()
                .map(entry -> EqExpr.create2(entry.getKey().getRef(), ExprUtils.applyPrimes(entry.getValue().getRef(),VarIndexingFactory.indexing(1))))
                .toList();

        var Ts = And(
                ExprUtils.changeDecls(i,varMap),
                ExprUtils.changeDecls(t,varMap),
                And(varEqTempVar)
        );

        return new STS(Ts,transExpr,Not(targetExpr));
    }

    private Valuation extractReachedNotQState(SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> result) {
        Trace<Valuation, StsAction> trace =  result.asUnsafe().getCex();
        Valuation val = trace.getStates().get(trace.getStates().size() - 1);

        Map<Decl<?>,LitExpr<?>> filteredMap = val.toMap().entrySet().stream()
                .filter(entry -> !entry.getKey().getName().contains("_temp"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return ImmutableValuation.from(filteredMap);
    }

    private SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> checkReachability(STS sts) {
        StsConfig<? extends State, ? extends Action, ? extends Prec> config =
                new StsConfigBuilder(StsConfigBuilder.Domain.EXPL, StsConfigBuilder.Refinement.FW_BIN_ITP, Z3LegacySolverFactory.getInstance())
                        .build(sts);

        baseChecker.setConfig(config,sts);
        var result =  baseChecker.check();
        return result;
    }

}
