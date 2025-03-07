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

import hu.bme.mit.theta.analysis.Cex;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.False;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.*;

public class RLiveChecker<P extends Prec> implements SafetyChecker<Proof, Cex, P> {

    private final Solver solver;
    private final SafetyChecker<?, ?, ?> baseChecker;
    private final Logger logger;
    private final MonolithicExpr monolithicExpr;
    private Set<ExprState> reachableQStates;
    Set<ExprState> c;

    public RLiveChecker(
            final MonolithicExpr monolithicExpr,
            final SolverManager solverManager,
            final SafetyChecker<?, ?, ?> baseChecker,
            final Logger logger) throws Exception {
        this.monolithicExpr = monolithicExpr;
        this.solver = solverManager.getSolverFactory("Z3").createSolver();
        this.baseChecker = baseChecker;
        this.logger = logger;
        this.reachableQStates = new HashSet<>();
    }

    public SafetyResult<Proof, Cex> check(P input) {

        c = new HashSet<>();


        while (true) {

            SafetyResult<?, ?> result = checkReachability();

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
            SafetyResult<?, ?> result = checkReachability();
            if(result.isUnsafe()){
                ExprState t = extractReachedNotQState(result);
                var combinedSet = new HashSet<>(reachableQStates);
                combinedSet.add(t);
                if (searchCex(t,combinedSet)){
                    return true;
                }
            } else {
                //get invariants and add to C
            }

        }

    }

    private ExprState extractReachedNotQState(SafetyResult<?,?> result) {
        return null;
    }

    private SafetyResult<?,?> checkReachability() {
        return null;
    }

}
