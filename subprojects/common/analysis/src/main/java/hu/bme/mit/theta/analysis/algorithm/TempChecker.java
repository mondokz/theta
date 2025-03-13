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
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.arg.ARG;
import hu.bme.mit.theta.analysis.algorithm.arg.ArgNode;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.z3legacy.Z3LegacySolverFactory;
import hu.bme.mit.theta.solver.z3legacy.Z3SolverManager;
import hu.bme.mit.theta.sts.STS;
import hu.bme.mit.theta.sts.analysis.StsAction;
import hu.bme.mit.theta.sts.analysis.StsTraceConcretizer;
import hu.bme.mit.theta.sts.analysis.config.StsConfig;

import java.util.List;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.False;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Or;

public class TempChecker <P extends Prec,Pr extends Proof, C extends Cex> implements SafetyChecker<InvariantForRlive, Trace<Valuation, StsAction>, P>{

    private StsConfig<ExprState,StsAction,P> config;
    private STS sts;

    public TempChecker() {
    }

    @Override
    public SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> check(P input) {
        SafetyResult<ARG<ExprState, StsAction>,Trace<ExprState, StsAction>> result = config.check();
        ARG<ExprState, StsAction> proof = result.getProof();
        var invariant = extractInvariant(proof);

        if(result.isUnsafe()){
            var cex = StsTraceConcretizer.concretize(sts, result.asUnsafe().getCex(), Z3LegacySolverFactory.getInstance());
            return SafetyResult.unsafe(cex,invariant);
        } else {
            return SafetyResult.safe(invariant);
        }


        }

    public InvariantForRlive extractInvariant(ARG<ExprState, StsAction> proof) {
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

    public void setConfig(StsConfig config, STS sts){
        this.config = config;
        this.sts = sts;
    }

    }
