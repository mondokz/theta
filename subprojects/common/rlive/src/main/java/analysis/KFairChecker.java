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
import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;
import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.ImmutableValuation;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.LitExpr;
import hu.bme.mit.theta.core.type.abstracttype.EqExpr;
import hu.bme.mit.theta.core.type.anytype.IteExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.inttype.IntType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.PathUtils;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;
import hu.bme.mit.theta.solver.UCSolver;
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
import static hu.bme.mit.theta.core.type.inttype.IntExprs.*;

public class KFairChecker<P extends Prec> implements SafetyChecker<Proof, Cex, P> {

    public enum Mode {
        K_FAIR,     // Uses both k-increment and loop detection
        FAIR,       // Only loop detection (no k-increment)
        K_LIVENESS  // Only k-increment (no loop detection)
    }

    private final TempChecker<?, ?, ?> baseChecker;
    private STS monolithicExpr;
    private STS tempMonolithicExpr;
    private Expr<BoolType> wallStates;
    private Expr<BoolType> c;
    private UCSolver solver;
    private VarDecl<IntType> violated;
    private final Mode mode;

    public KFairChecker(
            final STS monolithicExpr,
            final TempChecker<P, InvariantForRlive, Trace<Valuation, StsAction>> baseChecker,
            final Mode mode) throws Exception {
        this.tempMonolithicExpr = new STS(monolithicExpr.getInit(), monolithicExpr.getTrans(), Not(monolithicExpr.getProp()));
        this.baseChecker = baseChecker;
        this.mode = mode;
        solver = Z3LegacySolverFactory.getInstance().createUCSolver();
        violated = Decls.Var("__violated", Int());
        var newInit = And(tempMonolithicExpr.getInit(), Eq(violated.getRef(),Int(0)));
        var newTrans = And(tempMonolithicExpr.getTrans(),
                Eq(ExprUtils.applyPrimes(violated.getRef(),VarIndexingFactory.indexing(1)),
                        Add(violated.getRef(),
                                IteExpr.of(ExprUtils.applyPrimes(tempMonolithicExpr.getProp(), VarIndexingFactory.indexing(1)),Int(0),Int(1)))));

        this.monolithicExpr = new STS(newInit, newTrans, tempMonolithicExpr.getProp());
    }

    @Override
    public SafetyResult<Proof, Cex> check(P input) {
        int k = 0;
        c = False();
        wallStates = False();

        while (true) {
            // k+ times violated
            var kViol = Gt(violated.getRef(), Int(k));

            //  ¬q ∧ ¬C is satisfiable
            var target = And(kViol, Not(c));
            var prop = Not(target);
            try (WithPushPop wpp = new WithPushPop(solver)) {
                solver.track(PathUtils.unfold(prop, 0));
                if (solver.check().isUnsat()) {
                    return SafetyResult.safe(ARG.create(null));
                }
            }

            // REACHABILITY CHECK 1: Can we reach k violations?
            var sts = prepareReachabilityCheck(monolithicExpr.getInit(), monolithicExpr.getTrans(), prop);
            SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> result = checkReachability(sts);

            if (result.isSafe()) {
                return SafetyResult.safe(result.asSafe().getProof());
            }

            if (mode == Mode.K_LIVENESS) {
                k++;
                continue;
            }

            Valuation s = extractLastState(result);

            // REACHABILITY CHECK 2: Reachability of (T(s), T ∧ (W <-> W'), s)
            sts = prepareReachabilityCheck2(s.toExpr(), And(monolithicExpr.getTrans(), Iff(wallStates, ExprUtils.applyPrimes(wallStates, VarIndexingFactory.indexing(1)))), s.toExpr());
            result = checkReachability(sts);

            if (result.isUnsafe()) {
                return SafetyResult.unsafe(result.asUnsafe().getCex(), result.asUnsafe().getProof());
            } else {
                InvariantForRlive invariant = result.asSafe().getProof();
                var d = invariant.getInvariant();
                wallStates = Or(wallStates, d);

                Expr<BoolType> g = generalizingNoloop(s, d);
                c = Or(c, g);

                if (mode != Mode.FAIR) {
                    k++;
                }
            }
        }
    }

    private Expr<BoolType> generalizingNoloop(Valuation s, Expr<BoolType> d) {
        var tUnfold = PathUtils.unfold(monolithicExpr.getTrans(), 0);
        var notdUnfold = PathUtils.unfold(Not(ExprUtils.applyPrimes(d, VarIndexingFactory.indexing(1))), 0);
        var sUnfold = PathUtils.unfold(s.toExpr(), 0);
        var dUnfold = PathUtils.unfold(d, 0);

        Expr<BoolType> g1;
        Expr<BoolType> g2;

        try (WithPushPop wpp = new WithPushPop(solver)) {
            solver.track(tUnfold);
            solver.track(notdUnfold);
            solver.track(sUnfold);

            boolean isSat = solver.check().isSat();
            if (isSat) {
                throw new IllegalStateException("Expected UNSAT but got SAT");
            }
            var uc = new ArrayList<>(solver.getUnsatCore());
            uc.remove(tUnfold);
            uc.remove(notdUnfold);
            g1 = PathUtils.foldin(And(uc),0);
        }

        try (WithPushPop wpp = new WithPushPop(solver)) {
            solver.track(dUnfold);
            solver.track(sUnfold);

            boolean isSat = solver.check().isSat();
            if (isSat) {
                throw new IllegalStateException("Expected UNSAT but got SAT");
            }
            var uc = new ArrayList<>(solver.getUnsatCore());
            uc.remove(dUnfold);
            g2 = PathUtils.foldin(And(uc),0);
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
        Trace<Valuation, StsAction> trace =  result.asUnsafe().getCex();
        Valuation val = trace.getStates().get(trace.getStates().size() - 1);

        Map<Decl<?>, LitExpr<?>> filteredMap = val.toMap().entrySet().stream()
                .filter(entry -> !entry.getKey().getName().contains("__violated"))
                .filter(entry -> !entry.getKey().getName().contains("_temp"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return ImmutableValuation.from(filteredMap);
    }

    private SafetyResult<InvariantForRlive, Trace<Valuation, StsAction>> checkReachability(STS sts) {
        StsConfig<? extends State, ? extends Action, ? extends Prec> config =
                new StsConfigBuilder(StsConfigBuilder.Domain.EXPL, StsConfigBuilder.Refinement.FW_BIN_ITP, Z3LegacySolverFactory.getInstance())
                        .build(sts);

        baseChecker.setConfig(config, sts);
        return baseChecker.check();
    }
}