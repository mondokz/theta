package analysis;/*
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

import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Neq;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;
import static hu.bme.mit.theta.sts.analysis.config.StsConfigBuilder.Domain.*;
import static hu.bme.mit.theta.sts.analysis.config.StsConfigBuilder.Refinement.SEQ_ITP;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.cfa.analysis.CfaToMonolithicExprKt;
import hu.bme.mit.theta.cfa.dsl.CfaDslManager;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.z3legacy.Z3LegacySolverFactory;
import hu.bme.mit.theta.sts.STS;
import hu.bme.mit.theta.sts.aiger.AigerParser2;
import hu.bme.mit.theta.sts.aiger.AigerToSts;
import hu.bme.mit.theta.sts.analysis.config.StsConfig;
import hu.bme.mit.theta.sts.analysis.config.StsConfigBuilder;
import hu.bme.mit.theta.sts.dsl.StsDslManager;
import hu.bme.mit.theta.sts.dsl.StsSpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import hu.bme.mit.theta.sts.vmt.VmtToStsConverter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class RLiveTest {

    @Parameterized.Parameter(value = 0)
    public String filePath;

    @Parameterized.Parameter(value = 1)
    public StsConfigBuilder.Domain domain;

    @Parameterized.Parameter(value = 2)
    public StsConfigBuilder.Refinement refinement;

    @Parameterized.Parameter(value = 3)
    public boolean isSafe;

    @Parameterized.Parameter(value = 4)
    public List<Integer> acceptingStateIds;

    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}, {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                        // CFA Edge Case Tests - Simple, debuggable liveness checking scenarios
                        {"src/test/resources/test1.cfa", PRED_CART, SEQ_ITP, false, List.of(2,8)},
                        {"src/test/resources/test2.cfa", PRED_CART, SEQ_ITP, true, List.of(2,4)},
                        {"src/test/resources/test3.cfa", PRED_CART, SEQ_ITP, false, List.of(2,4,8)},
//                        {"""
//                            ; Counter system specification
//                            (declare-const x Int)
//                            (declare-const x.next Int)
//                            (define-fun sv.x () Int (! x :next x.next))
//                            (define-fun init () Bool (! (= x 0) :init true))
//                            (define-fun trans () Bool
//                               (! (or (and (< x 10) (or (= x.next (+ x 1)) (= x.next 0)))
//                                      (and (>= x 10) (= x.next 0))) :trans true))
//                            (define-fun invariant () Bool (! (>= x 0) :invar-property 0))
//                            (define-fun property () Bool (! (<= x 10) :live-property 1))
//                            """, PRED_CART, SEQ_ITP, true, List.of()},
//                         {"src/test/resources/testtest.system", PRED_CART, SEQ_ITP, true, List.of()},
//                        {"src/test/resources/counter2.system", PRED_CART, SEQ_ITP, false, List.of()},
//                        {"src/test/resources/counter3.system", PRED_CART, SEQ_ITP, true, List.of()},
//                        {"src/test/resources/counter4.system", PRED_CART, SEQ_ITP, false, List.of()},
//                        {"src/test/resources/test.system", PRED_CART, SEQ_ITP, true, List.of()}
//                        {"src/test/resources/byte_add_1_safe.c.aig", PRED_CART, SEQ_ITP, true, List.of(2,8)},
//                        {"src/test/resources/byte_add_unsafe.c.aig", PRED_CART, SEQ_ITP, false, List.of(2,4)},
//                        {"src/test/resources/interleave_bits_safe.c.aig", PRED_CART, SEQ_ITP, true, List.of(2,4,8)},
                });
    }

    public void test() throws Exception {
        STS sts = null;
        if (filePath.endsWith("aag")) {
            sts = AigerToSts.createSts(AigerParser2.parse(filePath));
        } else {
            final StsSpec spec = StsDslManager.createStsSpec(new FileInputStream(filePath));
            if (spec.getAllSts().size() != 1) {
                throw new UnsupportedOperationException("STS contains multiple properties.");
            }
            sts = Utils.singleElementOf(spec.getAllSts());
        }
        StsConfig<? extends State, ? extends Action, ? extends Prec> config =
                new StsConfigBuilder(domain, refinement, Z3LegacySolverFactory.getInstance())
                        .build(sts);

        var x = new RLiveChecker<ExplPrec>(sts,new TempChecker<>(),true);

        Assert.assertEquals(isSafe, x.check().isSafe());
    }
//    @Test
    public void testKFair() throws Exception {
        STS sts;
        if(filePath.endsWith("cfa")) {
            CFA cfa = CfaDslManager.createCfa(new FileInputStream(filePath));
            Expr<BoolType> prop = True();
            var stsAsMono = CfaToMonolithicExprKt.toMonolithicExpr(cfa);
            var pos = stsAsMono.getVars().stream().findFirst().get();
            for (var x : acceptingStateIds) {
                prop = And(prop, Neq(pos.getRef(),Int(x)));
            }
            sts = new STS(stsAsMono.getInitExpr(), stsAsMono.getTransExpr(), prop);
        } else {
            final StsSpec spec = StsDslManager.createStsSpec(new FileInputStream(filePath));
            if (spec.getAllSts().size() != 1) {
                throw new UnsupportedOperationException("STS contains multiple properties.");
            }
            sts = Utils.singleElementOf(spec.getAllSts());
        }

        var kFairChecker = new KFairChecker<ExplPrec>(sts,new TempChecker<>());

        Assert.assertEquals(isSafe, kFairChecker.check().isSafe());

    }
    @Test
    public void testRlive() throws Exception {
        STS sts;
        if(filePath.endsWith("cfa")) {
            CFA cfa = CfaDslManager.createCfa(new FileInputStream(filePath));
            Expr<BoolType> prop = True();
            var stsAsMono = CfaToMonolithicExprKt.toMonolithicExpr(cfa);
            var pos = stsAsMono.getVars().stream().findFirst().get();
            for (var x : acceptingStateIds) {
                prop = And(prop, Neq(pos.getRef(),Int(x)));
            }
            sts = new STS(stsAsMono.getInitExpr(), stsAsMono.getTransExpr(), prop);
        } else {
            final StsSpec spec = StsDslManager.createStsSpec(new FileInputStream(filePath));
            if (spec.getAllSts().size() != 1) {
                throw new UnsupportedOperationException("STS contains multiple properties.");
            }
            sts = Utils.singleElementOf(spec.getAllSts());
        }


        var rLiveChecker = new RLiveChecker<ExplPrec>(sts,new TempChecker<>(), true);

        Assert.assertEquals(isSafe, rLiveChecker.check().isSafe());
    }


//    @Test
    public void testRlivewithAiger() throws Exception {
        if (filePath == null || !filePath.endsWith("aig")) {
            // skip non-aag parameter sets
            return;
        }
        final STS sts = AigerToSts.createSts(AigerParser2.parse(filePath));
        var rLiveChecker = new RLiveChecker<ExplPrec>(sts, new TempChecker<>(), true);
        Assert.assertEquals(isSafe, rLiveChecker.check().isSafe());
    }




}
