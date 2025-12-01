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
package hu.bme.mit.theta.sts.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Stopwatch;
import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Cex;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.algorithm.bounded.BoundedCheckerBuilderKt;
import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.l2s.MonolithicL2SKt;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.common.logging.ConsoleLogger;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.SolverManager;
import hu.bme.mit.theta.solver.z3legacy.Z3LegacySolverFactory;
import hu.bme.mit.theta.sts.STS;
import hu.bme.mit.theta.sts.aiger.AigerParser2;
import hu.bme.mit.theta.sts.aiger.AigerToSts;
import hu.bme.mit.theta.sts.aiger.elements.AigerSystem;
import hu.bme.mit.theta.sts.analysis.StsToMonolithicExprKt;
import hu.bme.mit.theta.sts.analysis.config.StsConfig;
import hu.bme.mit.theta.sts.analysis.config.StsConfigBuilder;
import hu.bme.mit.theta.sts.dsl.StsDslManager;
import hu.bme.mit.theta.sts.dsl.StsSpec;
import analysis.KFairChecker;
import analysis.RLiveChecker;
import analysis.TempChecker;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Command-line interface for running liveness checking algorithms on STS models.
 * Supports multiple liveness algorithms and configurable solver backends.
 */
public class LivenessStsCli {

    private static final String JAR_NAME = "theta-liveness-sts-cli.jar";
    private final String[] args;

    enum Algorithm {
        RLIVE,
        KFAIR,
        K_LIVENESS,
        FAIR,
        L2S
    }

    enum SafetyChecker {
        CEGAR,
        IC3,
        BMC,
        K_IND,
        IMC
    }

    @Parameter(names = {"--model"}, description = "Path of the input AIG (.aag) (UTF_16) model", required = true)
    String model;

    @Parameter(names = {"--algorithm"}, description = "Liveness checking algorithm to use")
    Algorithm algorithm = Algorithm.RLIVE;

    @Parameter(names = {"--checker"}, description = "Checker strategy to use")
    SafetyChecker safetyChecker = SafetyChecker.CEGAR;

    @Parameter(names = {"--solver"}, description = "Solver")
    String solver = "Z3";

    @Parameter(names = {"--prune"}, description = "Enable pruning for rlive")
    boolean prune = true;

    @Parameter(
            names = {"--loglevel"},
            description = "Detailedness of logging")
    Logger.Level logLevel = Logger.Level.SUBSTEP;

    @Parameter(
            names = {"--benchmark"},
            description = "Benchmark mode")
    Boolean benchmarkMode = false;

    private Logger logger;

    public LivenessStsCli(final String[] args) {
        this.args = args;
    }

    public static void main(final String[] args) {
        final LivenessStsCli cli = new LivenessStsCli(args);
        cli.run();
    }

    private void run() {
        final JCommander jCommander = JCommander.newBuilder()
                .addObject(this)
                .programName(JAR_NAME)
                .build();
        logger = benchmarkMode ? NullLogger.getInstance() : new ConsoleLogger(logLevel);
        try {
            jCommander.parse(args);
        } catch (ParameterException ex) {
            System.out.println("Invalid parameters: " + ex.getMessage());
            jCommander.usage();
            return;
        }

        try {
            final Stopwatch sw = Stopwatch.createStarted();
            final STS sts = loadModel();
            final SolverFactory solverFactory = getSolverFactory();
            final SafetyResult<?, ? extends Cex> result = runAlgorithm(sts, solverFactory);
            sw.stop();
            printResult(result, sts, sw.elapsed(TimeUnit.MILLISECONDS));
        } catch (Throwable t) {
            printError(t);
            System.exit(1);
        }
    }

    private STS loadModel() throws Exception {
        try {
            if (model.endsWith(".aag")) {
                final AigerSystem aigerSystem = AigerParser2.parse(model);
                return AigerToSts.createLivenessSts(aigerSystem, 0);
            } else {
                try (InputStream is = new FileInputStream(model)) {
                    final StsSpec spec = StsDslManager.createStsSpec(is);
                    System.out.println("runnin3");
                    if (spec.getAllSts().size() != 1) {
                        throw new UnsupportedOperationException("STS contains multiple properties");
                    }
                    return Utils.singleElementOf(spec.getAllSts());
                }
            }
        } catch (Exception ex) {
            throw new Exception("Could not parse model: " + ex.getMessage(), ex);
        }
    }

    private SolverFactory getSolverFactory() throws Exception {
        try {
            //TODO SolverManager.resolveSolverFactory didnt work
            return Z3LegacySolverFactory.getInstance();
        } catch (Exception ex) {
            throw new Exception("Could not resolve solver '" + solver + "': " + ex.getMessage(), ex);
        }
    }

    private SafetyResult<?, ? extends Cex> runAlgorithm(final STS sts, final SolverFactory solverFactory) throws Exception {
        switch (algorithm) {
            case RLIVE:
                return runRLive(sts, solverFactory);
            case KFAIR:
                return runKFair(sts, solverFactory);
            case K_LIVENESS:
                return runKLiveness(sts, solverFactory);
            case FAIR:
                return runFair(sts, solverFactory);
            case L2S:
                return runL2S(sts, solverFactory);
            default:
                throw new UnsupportedOperationException("Algorithm " + algorithm + " not supported");
        }
    }

    private SafetyResult<?, ? extends Cex> runRLive(final STS sts, final SolverFactory solverFactory) throws Exception {
        final RLiveChecker<ExplPrec> checker = new RLiveChecker<>(StsToMonolithicExprKt.toMonolithicExpr(sts), new TempChecker<>(), prune, safetyChecker.equals(SafetyChecker.IC3), solverFactory, logger);
        return checker.check();
    }

    private SafetyResult<?, ? extends Cex> runKFair(final STS sts, final SolverFactory solverFactory) throws Exception {
        final KFairChecker<ExplPrec> checker = new KFairChecker<>(sts, new TempChecker<>(), KFairChecker.Mode.K_FAIR);
        return checker.check();
    }

    private SafetyResult<?, ? extends Cex> runKLiveness(final STS sts, final SolverFactory solverFactory) throws Exception {
        final KFairChecker<ExplPrec> checker = new KFairChecker<>(sts, new TempChecker<>(), KFairChecker.Mode.K_LIVENESS);
        return checker.check();
    }

    private SafetyResult<?, ? extends Cex> runFair(final STS sts, final SolverFactory solverFactory) throws Exception {
        final KFairChecker<ExplPrec> checker = new KFairChecker<>(sts, new TempChecker<>(), KFairChecker.Mode.FAIR);
        return checker.check();
    }

    private SafetyResult<?, ? extends Cex> runL2S(final STS sts, final SolverFactory solverFactory) throws Exception {
        var indexingBuilder = VarIndexingFactory.basicIndexingBuilder(0);
        for (VarDecl<?> v : sts.getVars()) {
            indexingBuilder.inc(v);
        }
        var mon = MonolithicL2SKt.createMonolithicL2S(new MonolithicExpr(
                sts.getInit(), sts.getTrans(), sts.getProp(), indexingBuilder.build()
        ));

        switch (safetyChecker) {
            case CEGAR:
                return runL2SWithCegar(mon, sts, solverFactory);
            case IC3:
                throw new UnsupportedOperationException("TODO merge ic3 branch");
            case BMC:
                return runL2SWithBMC(mon, sts, solverFactory);
            case K_IND:
                return runL2SWithKIND(mon, sts, solverFactory);
            case IMC:
                return runL2SWithIMC(mon, sts, solverFactory);
            default:
                throw new UnsupportedOperationException("Checker " + safetyChecker + " not supported for L2S");
        }
    }

    private SafetyResult<?, ? extends Cex> runL2SWithCegar(final MonolithicExpr mon, final STS sts, final SolverFactory solverFactory) throws Exception {
        StsConfig<? extends State, ? extends Action, ? extends Prec> config =
                new StsConfigBuilder(StsConfigBuilder.Domain.EXPL, StsConfigBuilder.Refinement.FW_BIN_ITP, Z3LegacySolverFactory.getInstance())
                        .build(sts);

        return config.check();
    }

    private SafetyResult<?, ? extends Cex> runL2SWithBMC(final MonolithicExpr mon, final STS sts, final SolverFactory solverFactory) throws Exception {
        return BoundedCheckerBuilderKt.buildBMC(
                mon,
                solverFactory.createSolver(),
                val -> StsToMonolithicExprKt.valToState(sts, val),
                (val1, val2) -> StsToMonolithicExprKt.valToAction(sts, val1, val2),
                logger
        ).check();
    }

    private SafetyResult<?, ? extends Cex> runL2SWithKIND(final MonolithicExpr mon, final STS sts, final SolverFactory solverFactory) throws Exception {
        return BoundedCheckerBuilderKt.buildKIND(
                mon,
                solverFactory.createSolver(),
                solverFactory.createSolver(),
                val -> StsToMonolithicExprKt.valToState(sts, val),
                (val1, val2) -> StsToMonolithicExprKt.valToAction(sts, val1, val2),
                logger
        ).check();
    }

    private SafetyResult<?, ? extends Cex> runL2SWithIMC(final MonolithicExpr mon, final STS sts, final SolverFactory solverFactory) throws Exception {
        return BoundedCheckerBuilderKt.buildIMC(
                mon,
                solverFactory.createSolver(),
                solverFactory.createItpSolver(),
                val -> StsToMonolithicExprKt.valToState(sts, val),
                (val1, val2) -> StsToMonolithicExprKt.valToAction(sts, val1, val2),
                logger
        ).check();
    }

    private void printResult(final SafetyResult<?, ? extends Cex> status, final STS sts, final long timeMs) {
            System.out.println("========================================");
            System.out.println("Result: " + (status.isSafe() ? "SAFE" : "UNSAFE"));
            System.out.println("Algorithm: " + algorithm);
            System.out.println("Checker: " + safetyChecker);
            System.out.println("Solver: " + solver);
            System.out.println("Time (ms): " + timeMs);
            System.out.println("Variables: " + sts.getVars().size());
            if (status.isUnsafe()) {
            }
            System.out.println("========================================");
    }

    private void printError(final Throwable ex) {
        System.err.println("========================================");
        System.err.println("ERROR: " + ex.getMessage());
        System.err.println("========================================");
    }
}

