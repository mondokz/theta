/*
 *  Copyright 2024 Budapest University of Technology and Economics
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
package hu.bme.mit.theta.cfa.analysis;

import hu.bme.mit.theta.analysis.*;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.bounded.BoundedChecker;
import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;
import hu.bme.mit.theta.analysis.expl.ExplInitFunc;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expl.ExplStmtAnalysis;
import hu.bme.mit.theta.analysis.l2s.L2STransform;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfig;
import hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder;
import hu.bme.mit.theta.cfa.analysis.lts.CfaSbeLts;
import hu.bme.mit.theta.cfa.analysis.prec.GlobalCfaPrec;
import hu.bme.mit.theta.cfa.dsl.CfaDslManager;
import hu.bme.mit.theta.common.OsHelper;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.SolverManager;
import hu.bme.mit.theta.solver.smtlib.SmtLibSolverManager;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.solver.z3.Z3SolverManager;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.Domain.EXPL;
import static hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.Domain.PRED_BOOL;
import static hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.Domain.PRED_CART;
import static hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.Refinement.BW_BIN_ITP;
import static hu.bme.mit.theta.cfa.analysis.config.CfaConfigBuilder.Refinement.SEQ_ITP;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;

@RunWith(value = Parameterized.class)
public class CfaTest {

    @Parameterized.Parameter(value = 0)
    public String filePath;

    @Parameterized.Parameter(value = 1)
    public CfaConfigBuilder.Domain domain;

    @Parameterized.Parameter(value = 2)
    public CfaConfigBuilder.Refinement refinement;

    @Parameterized.Parameter(value = 3)
    public boolean isSafe;

    @Parameterized.Parameter(value = 4)
    public int cexLength;

    @Parameterized.Parameter(value = 5)
    public String solver;

    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}, {3}, {4}, {5}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{

                /*{"src/test/resources/arithmetic-bool00.cfa", PRED_CART, SEQ_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-bool00.cfa", PRED_BOOL, BW_BIN_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-bool00.cfa", EXPL, SEQ_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-bool01.cfa", PRED_CART, SEQ_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-bool01.cfa", PRED_BOOL, BW_BIN_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-bool01.cfa", EXPL, SEQ_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-bool10.cfa", PRED_BOOL, SEQ_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-bool10.cfa", PRED_CART, BW_BIN_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-bool10.cfa", EXPL, SEQ_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-bool11.cfa", PRED_CART, SEQ_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-bool11.cfa", PRED_BOOL, BW_BIN_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-bool11.cfa", EXPL, SEQ_ITP, false, 15, "Z3"},

                {"src/test/resources/arithmetic-int.cfa", PRED_CART, SEQ_ITP, false, 13, "Z3"},

                {"src/test/resources/arithmetic-int.cfa", PRED_BOOL, BW_BIN_ITP, false, 13, "Z3"},

                {"src/test/resources/arithmetic-int.cfa", EXPL, SEQ_ITP, false, 13, "Z3"},

                {"src/test/resources/arithmetic-mod.cfa", PRED_CART, SEQ_ITP, true, 0, "Z3"},

                {"src/test/resources/arithmetic-mod.cfa", EXPL, BW_BIN_ITP, true, 0, "Z3"},

                {"src/test/resources/arrays.cfa", PRED_CART, SEQ_ITP, false, 8, "Z3"},

                {"src/test/resources/arrays.cfa", PRED_BOOL, BW_BIN_ITP, false, 8, "Z3"},

                {"src/test/resources/arrayinit.cfa", PRED_CART, BW_BIN_ITP, false, 3, "Z3"},

                {"src/test/resources/arrays.cfa", EXPL, SEQ_ITP, false, 8, "Z3"},

                {"src/test/resources/counter5_true.cfa", PRED_BOOL, SEQ_ITP, true, 0, "Z3"},

                {"src/test/resources/counter5_true.cfa", PRED_CART, BW_BIN_ITP, true, 0, "Z3"},

                {"src/test/resources/counter5_true.cfa", EXPL, SEQ_ITP, true, 0, "Z3"},

                {"src/test/resources/counter_bv_true.cfa", EXPL, NWT_IT_WP, true, 0, "Z3"},

                {"src/test/resources/counter_bv_false.cfa", EXPL, NWT_IT_WP, false, 13, "Z3"},

                {"src/test/resources/counter_bv_true.cfa", PRED_CART, NWT_IT_WP, true, 0, "Z3"},

                {"src/test/resources/counter_bv_false.cfa", PRED_CART, UCB, false, 13, "Z3"},

                {"src/test/resources/counter_bv_true.cfa", EXPL, SEQ_ITP, true, 0, "mathsat:latest"},

                {"src/test/resources/counter_bv_false.cfa", EXPL, SEQ_ITP, false, 13, "mathsat:latest"},

                {"src/test/resources/fp1.cfa", PRED_CART, NWT_IT_WP, true, 0, "Z3"},

                {"src/test/resources/fp2.cfa", PRED_CART, NWT_IT_WP, false, 5, "Z3"},

                {"src/test/resources/counter_fp_true.cfa", EXPL, NWT_IT_WP, true, 0, "Z3"},

                {"src/test/resources/ifelse.cfa", PRED_CART, SEQ_ITP, false, 3, "Z3"},

                {"src/test/resources/ifelse.cfa", PRED_BOOL, BW_BIN_ITP, false, 3, "Z3"},

                {"src/test/resources/ifelse.cfa", EXPL, SEQ_ITP, false, 3, "Z3"},

                {"src/test/resources/locking.cfa", PRED_CART, SEQ_ITP, true, 0, "Z3"},*/

                {"src/test/resources/looptest.cfa", PRED_CART, SEQ_ITP, false, 3, "Z3"},

                {"src/test/resources/nolooptest1.cfa", PRED_BOOL, BW_BIN_ITP, true, 3, "Z3"},

                {"src/test/resources/nolooptest2.cfa", EXPL, SEQ_ITP, true, 3, "Z3"}

        });
    }

    @Test
    public void test() throws Exception {
        SolverManager.registerSolverManager(Z3SolverManager.create());
        if (OsHelper.getOs().equals(OsHelper.OperatingSystem.LINUX)) {
            SolverManager.registerSolverManager(
                    SmtLibSolverManager.create(SmtLibSolverManager.HOME, NullLogger.getInstance()));
        }

        final SolverFactory solverFactory;
        try {
            solverFactory = SolverManager.resolveSolverFactory(solver);
        } catch (Exception e) {
            Assume.assumeNoException(e);
            return;
        }

        try {
            CFA cfa = CfaDslManager.createCfa(new FileInputStream(filePath));
            var cfaMono = CfaToMonoliticTransFunc.create(cfa);
            var monoltihicExpr = new MonolithicExpr(cfaMono.getInitExpr(),cfaMono.getTransExpr(), cfaMono.getPropExpr(), cfaMono.getOffsetIndexing());
            var ltsTrafo = new L2STransform(monoltihicExpr);
            var mExpr = new MonolithicExpr(ltsTrafo.getInitFunc(),ltsTrafo.getTransFunc(),ltsTrafo.getProp(), ltsTrafo.getOffsetIndexing());
            var indSolver = Z3SolverFactory.getInstance().createSolver();
            var solver1 = Z3SolverFactory.getInstance().createSolver();
            var itpSolver = Z3SolverFactory.getInstance().createItpSolver();

            var checker = new BoundedChecker<>(
                    mExpr,
                    (x) -> (false),
                    solver1,
                    () -> (true),
                    () -> (true),
                    itpSolver,
                    (a) -> (false),
                    indSolver,
                    (a) -> (true),
                    (valuation) -> ExplState.of((Valuation)valuation),
                    (v2,v1)->  new Stub(Collections.emptyList()),
                    new ConsoleLogger(Logger.Level.VERBOSE)
            );
            CfaConfig<? extends State, ? extends Action, ? extends Prec> config
                    = new CfaConfigBuilder(domain, refinement, solverFactory).build(cfa,
                    cfa.getErrorLoc().get());
            SafetyResult<? extends State, ? extends Action> result = config.check();
            var res = checker.check();
            Assert.assertEquals(isSafe, res.isSafe());

        } finally {
            SolverManager.closeAll();
        }
    }

    @Test
    public void testL2S() throws Exception {
        SolverManager.registerSolverManager(Z3SolverManager.create());
        if (OsHelper.getOs().equals(OsHelper.OperatingSystem.LINUX)) {
            SolverManager.registerSolverManager(
                    SmtLibSolverManager.create(SmtLibSolverManager.HOME, NullLogger.getInstance()));
        }
        try {
            CFA cfa = CfaDslManager.createCfa(new FileInputStream(filePath));
            var solver1 = Z3SolverFactory.getInstance().createSolver();
            LTS<CfaState<?>,CfaAction> baselts = CfaSbeLts.getInstance();
            final Analysis<CfaState<ExplState>, CfaAction, CfaPrec<ExplPrec>> analysis = CfaAnalysis
                    .create(cfa.getInitLoc(),
                            ExplStmtAnalysis.create(solver1, True(),
                                    1));
            CfaL2S<ExplState> cfal2s = CfaL2S.create(
                    baselts,
                    cfa,
                    cfa.getVars()
            );
            final Logger logger = new ConsoleLogger(Logger.Level.SUBSTEP);
            var fullPrec = GlobalCfaPrec.create(ExplPrec.of(cfal2s.getAllVars()));
            var transFunc = analysis.getTransFunc();
            var initFunc = CfaInitFunc.create(
                    cfa.getInitLoc(), ExplInitFunc.create(solver1, cfal2s.getInitExpr())
            );
            var initStates = initFunc.getInitStates(fullPrec);
            var state = initStates.stream().findFirst().get();
            var actions = cfal2s.getEnabledActionsFor(state);
            for (var act: actions) {
                var succStates = transFunc.getSuccStates(state,act,fullPrec);
                logger.write(Logger.Level.INFO,succStates.toString());
                for (var st1 : succStates){
                    var acts2 = cfal2s.getEnabledActionsFor(st1);
                    for(var ac2 : acts2){
                        var succstates2 = transFunc.getSuccStates(st1,ac2,fullPrec);
                        logger.write(Logger.Level.INFO,succstates2.toString());
                        for (var st2 : succstates2){
                            var acts3 = cfal2s.getEnabledActionsFor(st2);
                            for(var ac3 : acts3){
                                var succstates3 = transFunc.getSuccStates(st2,ac3,fullPrec);
                                logger.write(Logger.Level.INFO,succstates2.toString());
                            }
                        }
                    }
                }
            }



        } finally {
            SolverManager.closeAll();
        }
    }


}
