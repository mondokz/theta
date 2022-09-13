package hu.bme.mit.theta.gamma.frontend.dsl;

import hu.bme.mit.theta.xcfa.passes.XcfaPassManager;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class GammaDslManagerTest {

    @Test
    public void createCfa() throws IOException {
        XcfaPassManager.clearXCFAPasses();
        XcfaPassManager.clearProcedurePasses();
        XcfaPassManager.clearProcessPasses();

        String statechart = """
                statechart System_TimerStatechart [
                    port startA : provides Start
                    port stopA : provides Stop
                    port startB : provides Start
                    port stopB : provides Stop
                    port tickA : provides Tick
                    port tickB : provides Tick
                ] {
                    var time : integer := 0
                    region SubTimerStates {
                        state Ticking {
                            entry / {
                                time := time + 1;
                            }

                            region SubReg {
                                state SubState
                            }
                        }
                        state Paused
                        initial InitialTimerStatesOfSubTimerStates
                    }
                    transition from InitialTimerStatesOfSubTimerStates to Ticking
                    transition from Ticking to Ticking when tickA.in_TickTimer
                    transition from Ticking to Ticking when tickB.in_TickTimer
                    transition from Ticking to Paused when stopA.in_StopTimer
                    transition from Ticking to Paused when stopB.in_StopTimer
                    transition from Paused to Ticking when startA.in_StartTimer
                    transition from Paused to Ticking when startB.in_StartTimer
                }
                """;
        System.err.println(GammaDslManager.createCfa(statechart).toDot());
    }
}