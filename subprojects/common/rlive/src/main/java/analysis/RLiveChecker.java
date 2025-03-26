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
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.abstracttype.EqExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.z3legacy.Z3LegacySolverFactory;
import hu.bme.mit.theta.sts.STS;
import hu.bme.mit.theta.sts.analysis.StsAction;
import hu.bme.mit.theta.sts.analysis.config.StsConfig;
import hu.bme.mit.theta.sts.analysis.config.StsConfigBuilder;

import java.util.*;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.False;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.*;

public class RLiveChecker<P extends Prec> implements SafetyChecker<Proof, Cex, P> {
    private final TempChecker<?,?,?> baseChecker;
    private final STS monolithicExpr;
    private Set<Valuation> reachableQStates;
    Expr<BoolType> c;

    public RLiveChecker(
            final STS monolithicExpr,
            final TempChecker<P, InvariantForRlive, Trace<Valuation, StsAction>> baseChecker) throws Exception {
        this.monolithicExpr = monolithicExpr;
        this.baseChecker = baseChecker;
        this.reachableQStates = new HashSet<>();
    }

    public SafetyResult<Proof, Cex> check(P input) {

        c = False();

        while (true) {
            System.out.println("huh");
            var sts = prepareExpressions(monolithicExpr.getTrans(), monolithicExpr.getProp(), monolithicExpr.getInit());
            SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> result = checkReachability(sts);

            if (result.isUnsafe()) {
                Valuation s = extractReachedNotQState(result);
                if(searchCex(s,reachableQStates)){
                    return SafetyResult.unsafe(null,null);
                };
            }

        }
    }

    private boolean searchCex(Valuation s, Set<Valuation> reachableQStates) {
        if(reachableQStates.contains(s)){
            return true;
        }
        while(true){
            var sts = prepareExpressions(s.toExpr(), monolithicExpr.getProp(), monolithicExpr.getInit());
            SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> result = checkReachability(sts);;
            System.out.println("huh2");
            if(result.isUnsafe()){
                Valuation t = extractReachedNotQState(result);
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


    private STS prepareExpressions(Expr<BoolType> t, Expr<BoolType> q, Expr<BoolType> i) {
        var cPrime = ExprUtils.applyPrimes(c, VarIndexingFactory.indexing(1));
        var transExpr = And(t, And(Not(c), Not(cPrime)));
        var propertyExpr = And(Not(q), And(t, Not(cPrime)));

        STS tempSts = new STS(i,transExpr,propertyExpr);
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

        return new STS(Ts,transExpr,propertyExpr);
    }

    private Valuation extractReachedNotQState(SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> result) {
        Trace<Valuation, StsAction> trace =  result.asUnsafe().getCex();
        return trace.getStates().get(trace.getStates().size() - 1);
    }

    private SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> checkReachability(STS sts) {
        StsConfig<? extends State, ? extends Action, ? extends Prec> config =
                new StsConfigBuilder(StsConfigBuilder.Domain.EXPL, StsConfigBuilder.Refinement.FW_BIN_ITP, Z3LegacySolverFactory.getInstance())
                        .build(sts);

        baseChecker.setConfig(config,sts);
        return baseChecker.check();
    }

}
