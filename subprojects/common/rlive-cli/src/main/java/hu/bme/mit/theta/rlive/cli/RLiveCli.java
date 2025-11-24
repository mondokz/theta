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

package hu.bme.mit.theta.rlive.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Stopwatch;
import hu.bme.mit.theta.analysis.Cex;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.sts.aiger.AigerParser2;
import hu.bme.mit.theta.sts.aiger.AigerToSts;
import hu.bme.mit.theta.sts.aiger.elements.AigerSystem;
import hu.bme.mit.theta.sts.analysis.StsToMonolithicExprKt;
import hu.bme.mit.theta.sts.dsl.StsDslManager;
import hu.bme.mit.theta.sts.dsl.StsSpec;
import analysis.RLiveChecker;
import analysis.TempChecker;
import hu.bme.mit.theta.analysis.expl.ExplPrec;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public final class RLiveCli {

    private static final String JAR_NAME = "theta-rlive-cli.jar";
    private final String[] args;

    @Parameter(names = {"--model"}, description = "Path of the input STS or AIG (.aag) model", required = true)
    String model;

    @Parameter(names = {"--prune"}, description = "Enable pruning heuristics during search")
    boolean prune = true;

    public RLiveCli(final String[] args) {
        this.args = args;
    }

    public static void main(final String[] args) {
        final RLiveCli cli = new RLiveCli(args);
        cli.run();
    }

    private void run() {
        try {
            JCommander.newBuilder().addObject(this).programName(JAR_NAME).build().parse(args);
        } catch (ParameterException ex) {
            System.out.println("Invalid parameters: " + ex.getMessage());
            return;
        }

        try {
            final Stopwatch sw = Stopwatch.createStarted();
            final MonolithicExpr model = loadModel();
            final RLiveChecker<ExplPrec> checker = new RLiveChecker<>(model, new TempChecker<>(), prune);
            final SafetyResult<?, ? extends Cex> result = checker.check(null);
            sw.stop();
            printBasicResult(result, model, sw.elapsed(TimeUnit.MILLISECONDS));
        } catch (Throwable t) {
            System.out.println("[ERROR] " + t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "" : t.getMessage()));
            System.exit(1);
        }
    }

    private MonolithicExpr loadModel() throws Exception {
        try {
            if (model.endsWith(".aag")) {
                final AigerSystem aigerSystem = AigerParser2.parse(model);
                return StsToMonolithicExprKt.toMonolithicExpr(AigerToSts.createLivenessSts(aigerSystem, 0));
            } else {
                try (InputStream is = new FileInputStream(model)) {
                    final StsSpec spec = StsDslManager.createStsSpec(is);
                    if (spec.getAllSts().size() != 1) {
                        throw new UnsupportedOperationException("STS contains multiple properties");
                    }
                    return StsToMonolithicExprKt.toMonolithicExpr(Utils.singleElementOf(spec.getAllSts()));
                }
            }
        } catch (Exception ex) {
            throw new Exception("Could not parse model: " + ex.getMessage(), ex);
        }
    }

    private void printBasicResult(SafetyResult<?, ? extends Cex> status, MonolithicExpr sts, long timeMs) {
        System.out.println("\n Result: " + (status.isSafe() ? "SAFE" : "UNSAFE"));
        System.out.println("TimeMs: " + timeMs);
        System.out.println("Vars: " + sts.getVars().size());
        if (status.isUnsafe()) {
            System.out.println("cex length: " + status.asUnsafe().getCex().length());
        }
    }
}
