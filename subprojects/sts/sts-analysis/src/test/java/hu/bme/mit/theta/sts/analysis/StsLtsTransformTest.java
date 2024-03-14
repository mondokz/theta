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
package hu.bme.mit.theta.sts.analysis;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.bounded.BoundedChecker;
import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory;
import hu.bme.mit.theta.sts.STS;
import hu.bme.mit.theta.sts.aiger.AigerParser;
import hu.bme.mit.theta.sts.aiger.AigerToSts;
import hu.bme.mit.theta.sts.analysis.config.StsConfig;
import hu.bme.mit.theta.sts.analysis.config.StsConfigBuilder;
import hu.bme.mit.theta.sts.analysis.lts.LtsTransform;
import hu.bme.mit.theta.sts.dsl.StsDslManager;
import hu.bme.mit.theta.sts.dsl.StsSpec;
import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.stringtemplate.v4.ST;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static hu.bme.mit.theta.sts.analysis.config.StsConfigBuilder.Domain.*;
import static hu.bme.mit.theta.sts.analysis.config.StsConfigBuilder.Refinement.SEQ_ITP;

@RunWith(value = Parameterized.class)
public class StsLtsTransformTest {

    @Parameterized.Parameter(value = 0)
    public String filePath;

    @Parameterized.Parameter(value = 1)
    public StsConfigBuilder.Domain domain;

    @Parameterized.Parameter(value = 2)
    public StsConfigBuilder.Refinement refinement;

    @Parameterized.Parameter(value = 3)
    public boolean isSafe;

    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}, {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{

                {"src/test/resources/counter.system", PRED_CART, SEQ_ITP, true},
                {"src/test/resources/counter2.system", PRED_CART, SEQ_ITP, false},
                {"src/test/resources/counter3.system", PRED_CART, SEQ_ITP, false},
                {"src/test/resources/counter4.system", PRED_CART, SEQ_ITP, false},
                {"src/test/resources/counter_bad.system", PRED_CART, SEQ_ITP, false},

        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test() throws IOException {
        STS sts = null;

        final StsSpec spec = StsDslManager.createStsSpec(new FileInputStream(filePath));
        if (spec.getAllSts().size() != 1) {
            throw new UnsupportedOperationException("STS contains multiple properties.");
        }
        sts = Utils.singleElementOf(spec.getAllSts());

        var transformedSts = LtsTransform.lts(sts);
        var solver = Z3SolverFactory.getInstance().createSolver();
        var itpSolver = Z3SolverFactory.getInstance().createItpSolver();
        var indSolver = Z3SolverFactory.getInstance().createSolver();
        var monolithicExpr = new MonolithicExpr(transformedSts.get1(), transformedSts.get2(), transformedSts.get3(), VarIndexingFactory.indexing(1));

        var checker = new BoundedChecker(
                monolithicExpr,
                (x) -> (false),
                solver,
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


        Assert.assertEquals(isSafe, checker.check().isSafe());
    }

}

