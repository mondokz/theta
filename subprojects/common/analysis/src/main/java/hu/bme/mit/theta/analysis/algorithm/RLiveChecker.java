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

package hu.bme.mit.theta.analysis.algorithm;

import hu.bme.mit.theta.analysis.*;
import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.indexings.VarIndexing;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverManager;
import hu.bme.mit.theta.solver.z3legacy.Z3LegacySolverFactory;
import hu.bme.mit.theta.sts.STS;
import hu.bme.mit.theta.sts.analysis.config.StsConfig;
import hu.bme.mit.theta.sts.analysis.config.StsConfigBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.False;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.*;

public class RLiveChecker<P extends Prec> implements SafetyChecker<Proof, Cex, P> {

    private final Solver solver;
    private final TempChecker<?,?,?> baseChecker;
    private final Logger logger;
    private final MonolithicExpr monolithicExpr;
    private Set<ExprState> reachableQStates;
    Expr<BoolType> c;

    public RLiveChecker(
            final MonolithicExpr monolithicExpr,
            final SolverManager solverManager,
            final TempChecker<?,?,?> baseChecker,
            final Logger logger) throws Exception {
        this.monolithicExpr = monolithicExpr;
        this.solver = solverManager.getSolverFactory("Z3").createSolver();
        this.baseChecker = baseChecker;
        this.logger = logger;
        this.reachableQStates = new HashSet<>();
    }

    public SafetyResult<Proof, Cex> check(P input) {

        c = False();

        while (true) {

            SafetyResult<?, ?> result = checkReachability(monolithicExpr.getInitExpr(), monolithicExpr.getTransExpr(), monolithicExpr.getPropExpr());

            if (result.isUnsafe()) {
                ExprState s = extractReachedNotQState(result);
                if(searchCex(s,reachableQStates)){
                    return SafetyResult.unsafe(null,null);
                };
            }

        }
    }

    private boolean searchCex(ExprState s, Set<ExprState> reachableQStates) {
        if (reachableQStates.isEmpty()) {
            return false;
        }
        if(reachableQStates.contains(s)){
            return true;
        }
        while(true){
            SafetyResult<?, ?> result = checkReachability(s.toExpr(), monolithicExpr.getTransExpr(), monolithicExpr.getPropExpr());;

            if(result.isUnsafe()){
                ExprState t = extractReachedNotQState(result);
                var combinedSet = new HashSet<>(reachableQStates);
                combinedSet.add(t);
                if (searchCex(t,combinedSet)){
                    return true;
                }
            } else {
                var invariant = (InvariantForRlive)result.getProof();
                c = And(c,invariant.getInvariant());
                return false;
            }
        }
    }

    private ExprState extractReachedNotQState(SafetyResult<?,?> result) {
        Trace<?, ?> trace = (Trace<?, ?>) result.asUnsafe().getCex();
        State lastState = (State) trace.getStates().get(trace.getStates().size() - 1);
        ExprState exprState = (ExprState) lastState;
        reachableQStates.add(exprState);
        return exprState;
    }

    private SafetyResult<?,?> checkReachability(Expr<BoolType> i, Expr<BoolType> t, Expr<BoolType> q ) {
        var cPrime = ExprUtils.applyPrimes(c, VarIndexingFactory.indexing(1));
        var transExpr = And(t, And(Not(c), Not(cPrime)));
        var propertyExpr = And(Not(q), And(t, Not(cPrime)));

        STS sts = new STS(i,transExpr,propertyExpr);
        StsConfig<? extends State, ? extends Action, ? extends Prec> config =
                new StsConfigBuilder(StsConfigBuilder.Domain.EXPL, StsConfigBuilder.Refinement.FW_BIN_ITP, Z3LegacySolverFactory.getInstance())
                        .build(sts);

        baseChecker.setConfig(config);
        return baseChecker.check();
    }

}
