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
import hu.bme.mit.theta.analysis.algorithm.arg.ARG;
import hu.bme.mit.theta.analysis.algorithm.arg.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.cegar.Abstractor;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.utils.ProofVisualizer;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.*;

import java.util.List;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.False;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Or;

public class TempChecker <P extends Prec,Pr extends Proof, C extends Cex> implements SafetyChecker<InvariantForRlive, C, P>{

    private final ARG<ExprState, ExprAction> proof;
    private final SafetyChecker<?, ?, ?> checker;

    public TempChecker(ARG<ExprState, ExprAction> proof,
                       final Abstractor<P, Pr> abstractor,
                       final Refiner<P, Pr, C> refiner,
                       final Logger logger,
                       final ProofVisualizer<? super Pr> proofVisualizer,
                       SafetyChecker<?,?,?> checker) {
        this.proof = proof;
        this.checker = checker;
    }

    @Override
    public SafetyResult<InvariantForRlive, C> check(P input) {
        SafetyResult<?,?> result = checker.check();
        ARG<ExprState, ExprAction> proof = (ARG<ExprState, ExprAction>) result.getProof();
        return (SafetyResult<InvariantForRlive, C>) SafetyResult.unsafe(result.asUnsafe().getCex(),extractInvariant(proof));
        }

    public InvariantForRlive extractInvariant(ARG<ExprState, ExprAction> proof) {
        List<ExprState> allStates = proof.getNodes()
                .map(ArgNode::getState)
                .toList();

        if (allStates.isEmpty()) {
            return new InvariantForRlive(False());
        }

        List<Expr<BoolType>> stateExprs = allStates.stream()
                .map(ExprState::toExpr)
                .toList();

        return new InvariantForRlive(Or(stateExprs));
    }

    }
