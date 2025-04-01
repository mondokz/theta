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
import hu.bme.mit.theta.analysis.algorithm.arg.ARG;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.abstracttype.EqExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.booltype.SmartBoolExprs;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.PathUtils;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.utils.WithPushPop;
import hu.bme.mit.theta.solver.z3legacy.Z3LegacySolverFactory;
import hu.bme.mit.theta.sts.STS;
import hu.bme.mit.theta.sts.analysis.StsAction;
import hu.bme.mit.theta.sts.analysis.config.StsConfig;
import hu.bme.mit.theta.sts.analysis.config.StsConfigBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.False;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Iff;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.*;

public class KFairChecker<P extends Prec> implements SafetyChecker<Proof, Cex, P> {
    private final TempChecker<?, ?, ?> baseChecker;
    private final STS monolithicExpr;
    private Expr<BoolType> wallStates;
    private Expr<BoolType> c;
    private Solver solver;

    public KFairChecker(
            final STS monolithicExpr,
            final TempChecker<P, InvariantForRlive, Trace<Valuation, StsAction>> baseChecker) throws Exception {
        this.monolithicExpr = monolithicExpr;
        this.baseChecker = baseChecker;
        solver = Z3LegacySolverFactory.getInstance().createSolver();
    }

    @Override
    public SafetyResult<Proof, Cex> check(P input) {
        int k = 0;
        c = False();
        wallStates = False();

        while (true) {
            //  ¬q ∧ ¬C is satisfiable
            var prop = And(Not(monolithicExpr.getProp()), Not(c));
            try (WithPushPop wpp = new WithPushPop(solver)) {
                solver.add(prop);
                if (solver.check().isUnsat()) {
                    return SafetyResult.safe(ARG.create(null));
                }
            }


            // reachability of (I, T, ¬q ∧ ¬C)
            var sts = prepareReachabilityCheck(monolithicExpr.getInit(), monolithicExpr.getTrans(), prop);
            SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> result = checkReachability(sts);

            if (result.isSafe()) {
                return SafetyResult.safe(result.asSafe().getProof());
            }

            Valuation s = extractLastState(result);

            //reachability of (T(s), T ∧ (W - W'), s)
            sts = prepareReachabilityCheck2(s.toExpr(), And(monolithicExpr.getTrans(), Iff(wallStates, ExprUtils.applyPrimes(wallStates, VarIndexingFactory.indexing(1)))), s.toExpr());
            result = checkReachability(sts);

            if (result.isUnsafe()) {
                return SafetyResult.unsafe(result.asUnsafe().getCex(), result.asUnsafe().getProof());
            } else {
                InvariantForRlive invariant = result.asSafe().getProof();
                var d = invariant.getInvariant();
                wallStates = And(wallStates, d);

                Expr<BoolType> g = generalizingNoloop(s, d);
                c = Or(c, g);
                k++;
            }
        }
    }

    private Expr<BoolType> generalizingNoloop(Valuation s, Expr<BoolType> d) {
        Expr<BoolType> expr = And(
                                            And(
                                                monolithicExpr.getTrans(),
                                                Not(ExprUtils.applyPrimes(d, VarIndexingFactory.indexing(1)))),
                                            s.toExpr());

        Expr<BoolType> g1;
        Expr<BoolType> g2;

        try (WithPushPop wpp = new WithPushPop(solver)) {
            solver.add(expr);
            assert solver.check().isUnsat();
            g1 = solver.getModel().toExpr();
        }

        try (WithPushPop wpp = new WithPushPop(solver)) {
            solver.add(And(d,s.toExpr()));
            assert solver.check().isUnsat();
            g2 = solver.getModel().toExpr();
        }

        return And(g1, g2);
    }



    private STS prepareReachabilityCheck(Expr<BoolType> init, Expr<BoolType> trans, Expr<BoolType> prop) {
        return new STS(init, trans, prop);
    }

    private STS prepareReachabilityCheck2(Expr<BoolType> init, Expr<BoolType> transExpr, Expr<BoolType> prop) {
        STS tempSts = new STS(init,transExpr,Not(prop));
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
                ExprUtils.changeDecls(init,varMap),
                ExprUtils.changeDecls(transExpr,varMap),
                And(varEqTempVar)
        );

        return new STS(Ts,transExpr,Not(prop));
    }

    private Valuation extractLastState(SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> result) {
        Trace<Valuation, StsAction> trace = result.asUnsafe().getCex();
        return trace.getStates().get(trace.getStates().size() - 1);
    }

    private SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> checkReachability(STS sts) {
        StsConfig<? extends State, ? extends Action, ? extends Prec> config =
                new StsConfigBuilder(StsConfigBuilder.Domain.EXPL, StsConfigBuilder.Refinement.FW_BIN_ITP, Z3LegacySolverFactory.getInstance())
                        .build(sts);

        baseChecker.setConfig(config, sts);
        return baseChecker.check();
    }
}
