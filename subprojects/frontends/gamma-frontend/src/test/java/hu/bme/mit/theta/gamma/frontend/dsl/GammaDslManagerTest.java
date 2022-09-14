package hu.bme.mit.theta.gamma.frontend.dsl;

import hu.bme.mit.theta.xcfa.model.XCFA;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class GammaDslManagerTest {

    @Test
    public void createCfa() throws IOException {

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
                                         entry / time := time + 1;
                             
                                         region SubReg {
                                            initial E1
                                            state SubState
                                         }
                                     }
                                     state Paused
                                     initial InitialTimerStatesOfSubTimerStates
                                 }
                                 transition from E1 to SubState
                                 transition from InitialTimerStatesOfSubTimerStates to Ticking
                                 transition from Ticking to Ticking when tickA.in_TickTimer
                                 transition from Ticking to Ticking when tickB.in_TickTimer
                                 transition from Ticking to Paused when stopA.in_StopTimer
                                 transition from Ticking to Paused when stopB.in_StopTimer
                                 transition from Paused to Ticking when startA.in_StartTimer
                                 transition from Paused to Ticking when startB.in_StartTimer
                            }
                """;
        XCFA xcfa = GammaDslManager.createCfa(statechart);
        System.err.println(xcfa);
    }
}