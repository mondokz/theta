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
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.sts.analysis.config.StsConfig;

import java.util.List;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.False;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Or;

public class TempChecker <P extends Prec,Pr extends Proof, C extends Cex> implements SafetyChecker<Pr, C, P>{

    private StsConfig config;

    public TempChecker() {
    }

    @Override
    public SafetyResult<Pr, C> check(P input) {
        SafetyResult<?,?> result = config.check();
        if(result.isUnsafe()){
            return (SafetyResult<Pr, C>) result;
        } else {
            ARG<ExprState, ExprAction> proof = (ARG<ExprState, ExprAction>) result.getProof();
            return (SafetyResult<Pr, C>) SafetyResult.unsafe(result.asUnsafe().getCex(),extractInvariant(proof));
        }


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

    public void setConfig(StsConfig config){
        this.config = config;
    }

    }
