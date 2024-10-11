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
package hu.bme.mit.theta.xsts.analysis;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.algorithm.ArgBuilder;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.bounded.BoundedChecker;
import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;
import hu.bme.mit.theta.analysis.expl.*;
import hu.bme.mit.theta.analysis.l2s.L2STransform;
import hu.bme.mit.theta.analysis.runtimemonitor.container.CexHashStorage;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.Logger.Level;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.SolverManager;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.solver.z3.Z3SolverManager;
import hu.bme.mit.theta.xsts.XSTS;
import hu.bme.mit.theta.xsts.analysis.config.XstsConfig;
import hu.bme.mit.theta.xsts.analysis.config.XstsConfigBuilder;

import hu.bme.mit.theta.xsts.dsl.XstsDslManager;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.Assert.assertTrue;

@RunWith(value = Parameterized.class)
public class XstsTest {

    private static final String SOLVER_STRING = "Z3";
    @Parameterized.Parameter(value = 0)
    public String filePath;

    @Parameterized.Parameter(value = 1)
    public String propPath;

    @Parameterized.Parameter(value = 2)
    public boolean safe;

    @Parameterized.Parameter(value = 3)
    public XstsConfigBuilder.Domain domain;

    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}, {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{

//                {"src/test/resources/model/trafficlight.xsts",
//                        "src/test/resources/property/green_and_red.prop", true,
//                        XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/trafficlight.xsts",
//                        "src/test/resources/property/green_and_red.prop", true,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/trafficlight.xsts",
//                        "src/test/resources/property/green_and_red.prop", true,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/trafficlight_v2.xsts",
//                        "src/test/resources/property/green_and_red.prop", true,
//                        XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/trafficlight_v2.xsts",
//                        "src/test/resources/property/green_and_red.prop", true,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/trafficlight_v2.xsts",
//                        "src/test/resources/property/green_and_red.prop", true,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/counter5.xsts",
//                        "src/test/resources/property/x_between_0_and_5.prop", true,
//                        XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/counter5.xsts",
//                        "src/test/resources/property/x_between_0_and_5.prop", true,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/counter5.xsts",
//                        "src/test/resources/property/x_between_0_and_5.prop", true,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/counter5.xsts", "src/test/resources/property/x_eq_5.prop",
//                        false, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/counter5.xsts", "src/test/resources/property/x_eq_5.prop",
//                        false, XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/counter5.xsts", "src/test/resources/property/x_eq_5.prop",
//                        false, XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/x_and_y.xsts", "src/test/resources/property/x_geq_y.prop",
//                        true, XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/x_powers.xsts", "src/test/resources/property/x_even.prop",
//                        true, XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/cross_with.xsts", "src/test/resources/property/cross.prop",
//                        false, XstsConfigBuilder.Domain.PRED_CART},
//
////                { "src/test/resources/model/cross_with.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.EXPL},
//
////                { "src/test/resources/model/cross_with.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.PROD},
//
//                {"src/test/resources/model/cross_without.xsts",
//                        "src/test/resources/property/cross.prop", false,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
////                { "src/test/resources/model/cross_without.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.EXPL},
//
////                { "src/test/resources/model/cross_without.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.PROD},
//
//                {"src/test/resources/model/choices.xsts", "src/test/resources/property/choices.prop",
//                        false, XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/choices.xsts", "src/test/resources/property/choices.prop",
//                        false, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/choices.xsts", "src/test/resources/property/choices.prop",
//                        false, XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/literals.xsts", "src/test/resources/property/literals.prop",
//                        true, XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/literals.xsts", "src/test/resources/property/literals.prop",
//                        true, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/literals.xsts", "src/test/resources/property/literals.prop",
//                        true, XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/cross3.xsts", "src/test/resources/property/cross.prop",
//                        false, XstsConfigBuilder.Domain.PRED_CART},
//
////                { "src/test/resources/model/cross3.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.EXPL},
//
////                { "src/test/resources/model/cross3.xsts", "src/test/resources/property/cross.prop", false, XstsConfigBuilder.Domain.PROD},
//
//                {"src/test/resources/model/sequential.xsts",
//                        "src/test/resources/property/sequential.prop", true,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/sequential.xsts",
//                        "src/test/resources/property/sequential.prop", true, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/sequential.xsts",
//                        "src/test/resources/property/sequential.prop", true,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/sequential.xsts",
//                        "src/test/resources/property/sequential2.prop", false,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/sequential.xsts",
//                        "src/test/resources/property/sequential2.prop", false,
//                        XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/sequential.xsts",
//                        "src/test/resources/property/sequential2.prop", false,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/on_off_statemachine.xsts",
//                        "src/test/resources/property/on_off_statemachine.prop", false,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/on_off_statemachine.xsts",
//                        "src/test/resources/property/on_off_statemachine.prop", false,
//                        XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/on_off_statemachine.xsts",
//                        "src/test/resources/property/on_off_statemachine.prop", false,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/on_off_statemachine.xsts",
//                        "src/test/resources/property/on_off_statemachine2.prop", true,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/on_off_statemachine.xsts",
//                        "src/test/resources/property/on_off_statemachine2.prop", true,
//                        XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/on_off_statemachine.xsts",
//                        "src/test/resources/property/on_off_statemachine2.prop", true,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/on_off_statemachine.xsts",
//                        "src/test/resources/property/on_off_statemachine3.prop", false,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/on_off_statemachine.xsts",
//                        "src/test/resources/property/on_off_statemachine3.prop", false,
//                        XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/on_off_statemachine.xsts",
//                        "src/test/resources/property/on_off_statemachine3.prop", false,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_5.prop",
//                        false, XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_5.prop",
//                        false, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_5.prop",
//                        false, XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
////                { "src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_50.prop", false, XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_50.prop",
//                        false, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_50.prop",
//                        false, XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_51.prop",
//                        true, XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_51.prop",
//                        true, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/counter50.xsts", "src/test/resources/property/x_eq_51.prop",
//                        true, XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/count_up_down.xsts",
//                        "src/test/resources/property/count_up_down.prop", false,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/count_up_down.xsts",
//                        "src/test/resources/property/count_up_down.prop", false,
//                        XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/count_up_down.xsts",
//                        "src/test/resources/property/count_up_down.prop", false,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/count_up_down.xsts",
//                        "src/test/resources/property/count_up_down2.prop", true,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/count_up_down.xsts",
//                        "src/test/resources/property/count_up_down2.prop", true,
//                        XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/count_up_down.xsts",
//                        "src/test/resources/property/count_up_down2.prop", true,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/bhmr2007.xsts", "src/test/resources/property/bhmr2007.prop",
//                        true, XstsConfigBuilder.Domain.PRED_CART},
//
////                { "src/test/resources/model/bhmr2007.xsts", "src/test/resources/property/bhmr2007.prop", true, XstsConfigBuilder.Domain.EXPL},
//
////                { "src/test/resources/model/bhmr2007.xsts", "src/test/resources/property/bhmr2007.prop", true, XstsConfigBuilder.Domain.PROD},
//
//                {"src/test/resources/model/css2003.xsts", "src/test/resources/property/css2003.prop",
//                        true, XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/css2003.xsts", "src/test/resources/property/css2003.prop",
//                        true, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/css2003.xsts", "src/test/resources/property/css2003.prop",
//                        true, XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
////                { "src/test/resources/model/ort.xsts", "src/test/resources/property/x_gt_2.prop", false, XstsConfigBuilder.Domain.PRED_CART},
//
////                { "src/test/resources/model/ort2.xsts", "src/test/resources/property/ort2.prop", true, XstsConfigBuilder.Domain.PRED_CART},
//
////                { "src/test/resources/model/crossroad_composite.xsts", "src/test/resources/property/both_green.prop", true, XstsConfigBuilder.Domain.EXPL}
//
//                {"src/test/resources/model/array_counter.xsts",
//                        "src/test/resources/property/array_10.prop", false,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/array_counter.xsts",
//                        "src/test/resources/property/array_10.prop", false, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/array_counter.xsts",
//                        "src/test/resources/property/array_10.prop", false,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/array_constant.xsts",
//                        "src/test/resources/property/array_constant.prop", true,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/array_constant.xsts",
//                        "src/test/resources/property/array_constant.prop", true,
//                        XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/array_constant.xsts",
//                        "src/test/resources/property/array_constant.prop", true,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/localvars.xsts",
//                        "src/test/resources/property/localvars.prop", true,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/localvars.xsts",
//                        "src/test/resources/property/localvars.prop", true, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/localvars.xsts",
//                        "src/test/resources/property/localvars.prop", true,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/localvars2.xsts",
//                        "src/test/resources/property/localvars2.prop", true,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/localvars2.xsts",
//                        "src/test/resources/property/localvars2.prop", true, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/localvars2.xsts",
//                        "src/test/resources/property/localvars2.prop", true,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/loopxy.xsts", "src/test/resources/property/loopxy.prop",
//                        true, XstsConfigBuilder.Domain.EXPL},
//
//                {"src/test/resources/model/loopxy.xsts", "src/test/resources/property/loopxy.prop",
//                        true, XstsConfigBuilder.Domain.EXPL_PRED_COMBINED},
//
//                {"src/test/resources/model/loopxy.xsts", "src/test/resources/property/loopxy.prop",
//                        true, XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/arraywrite_sugar.xsts",
//                        "src/test/resources/property/arraywrite_sugar.prop", false,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/if1.xsts", "src/test/resources/property/if1.prop", true,
//                        XstsConfigBuilder.Domain.PRED_CART},
//
//                {"src/test/resources/model/if2.xsts", "src/test/resources/property/if2.prop", false,
//                        XstsConfigBuilder.Domain.EXPL_PRED_COMBINED}

                {"src/test/resources/model/l2stest1.xsts", "src/test/resources/property/l2stest1.prop", true,
                        XstsConfigBuilder.Domain.PRED_CART},

                {"src/test/resources/model/l2stest2.xsts", "src/test/resources/property/l2stest2.prop", false,
                        XstsConfigBuilder.Domain.PRED_CART},

                {"src/test/resources/model/l2stest3.xsts", "src/test/resources/property/l2stest3.prop", false,
                        XstsConfigBuilder.Domain.PRED_CART},

                {"src/test/resources/model/l2stest4.xsts", "src/test/resources/property/l2stest4.prop", false,
                        XstsConfigBuilder.Domain.PRED_CART}
        });
    }

    @Test
    public void testL2Smonolithic() throws IOException {

        final Logger logger = new ConsoleLogger(Level.SUBSTEP);

        XSTS xsts;
        try (InputStream inputStream = new SequenceInputStream(new FileInputStream(filePath),
                new FileInputStream(propPath))) {
            xsts = XstsDslManager.createXsts(inputStream);
        }
        var mono = XstsToMonoliticTransFunc.create(xsts);
        var m = new MonolithicExpr(mono.getInitExpr(), mono.getTransExpr(), mono.getPropExpr(), mono.getOffsetIndexing());
        var lts = new L2STransform(m);
        var monoltihicExpr = new MonolithicExpr(lts.getInitFunc(), lts.getTransFunc(), lts.getProp(),lts.getOffsetIndexing());
        var indSolver = Z3SolverFactory.getInstance().createSolver();
        var solver1 = Z3SolverFactory.getInstance().createSolver();
        var itpSolver = Z3SolverFactory.getInstance().createItpSolver();
        var checker = new BoundedChecker(
                monoltihicExpr,
                (x) -> (false),
                solver1,
                () -> (true),
                () -> (true),
                itpSolver,
                (a) -> (true),
                indSolver,
                (a) -> (true),
                (valuation) -> ExplState.of((Valuation)valuation),
                (v2,v1)->  new Stub(Collections.emptyList()),
                new ConsoleLogger(Logger.Level.VERBOSE)
        );
        Assert.assertEquals(safe, checker.check().isSafe());

    }

    public void testXstsL2s() throws IOException {

        final Logger logger = new ConsoleLogger(Level.SUBSTEP);

        XSTS xsts;
        try (InputStream inputStream = new SequenceInputStream(new FileInputStream(filePath),
                new FileInputStream(propPath))) {
            xsts = XstsDslManager.createXsts(inputStream);
        }
        var solver = Z3SolverFactory.getInstance().createSolver();
        var mono = XstsToMonoliticTransFunc.create(xsts);
        var m = new MonolithicExpr(mono.getInitExpr(), mono.getTransExpr(), mono.getPropExpr(), mono.getOffsetIndexing());
        var baseLts = XstsLts.create(xsts,XstsStmtOptimizer.create(ExplStmtOptimizer.getInstance()));
        var initFormula = xsts.getInitFormula();

        var xstsl2s = XstsL2S.create(
                baseLts,
                xsts.getProp(),
                (expr) -> XstsInitFunc.create(ExplInitFunc.create(solver, expr)),
                xsts.getInitFormula(),
                xsts.getVars()
        );
        var xstsAnalysis = XstsAnalysis.create(ExplStmtAnalysis.create(solver,xsts.getInitFormula(),0));
        var fullPrec = ExplPrec.of(xstsl2s.getAllVars());
        final Predicate<XstsState<ExplState>> target = new XstsStatePredicate<ExplStatePredicate, ExplState>(
                new ExplStatePredicate(xsts.getProp(), solver));
        var x0 = 0;
        var transFunc = xstsAnalysis.getTransFunc();
        var initStates = xstsl2s.getInitStates(fullPrec);
        XstsState<ExplState> state = initStates.stream().findFirst().get();
        var actions = xstsl2s.getEnabledActionsFor(state);
        for (var act: actions){
            var succStates = transFunc.getSuccStates(state,act,fullPrec);
            logger.write(Level.INFO,succStates.toString());
            for (var st1 : succStates){
                var acts2 = xstsl2s.getEnabledActionsFor(st1);
                for(var ac2 : acts2){
                    var succstates2 = transFunc.getSuccStates(st1,ac2,fullPrec);
                    logger.write(Level.INFO,succstates2.toString());
                    for (var st2 : succstates2){
                        var acts3 = xstsl2s.getEnabledActionsFor(st2);
                        for(var ac3 : acts3){
                            var succstates3 = transFunc.getSuccStates(st2,ac3,fullPrec);
                            logger.write(Level.INFO,succstates2.toString());
                        }
                    }
                }
            }
        }
        assertTrue(true);

    }

    @Test
    public void testCegar() throws Exception {

        final Logger logger = new ConsoleLogger(Level.SUBSTEP);

        final SolverFactory solverFactory;
        var solverManager = Z3SolverManager.create();
        try {
            solverFactory = solverManager.getSolverFactory("Z3");
        } catch (Exception e) {
            Assume.assumeNoException(e);
            return;
        }

        XSTS xsts;
        try (InputStream inputStream = new SequenceInputStream(new FileInputStream(filePath),
                new FileInputStream(propPath))) {
            xsts = XstsDslManager.createXsts(inputStream);
        }

        try {
            final XstsConfig<?, ?, ?> configuration = new XstsConfigBuilder(domain,
                    XstsConfigBuilder.Refinement.SEQ_ITP, solverFactory,
                    solverFactory).initPrec(XstsConfigBuilder.InitPrec.CTRL)
                    .optimizeStmts(XstsConfigBuilder.OptimizeStmts.ON)
                    .predSplit(XstsConfigBuilder.PredSplit.CONJUNCTS).maxEnum(250)
                    .autoExpl(XstsConfigBuilder.AutoExpl.NEWOPERANDS).logger(logger).build(xsts);
            final SafetyResult<?, ?> status = configuration.check();

            if (safe) {
                assertTrue(status.isSafe());
            } else {
                assertTrue(status.isUnsafe());
            }
        } finally {
            SolverManager.closeAll();
        }

    }

}
