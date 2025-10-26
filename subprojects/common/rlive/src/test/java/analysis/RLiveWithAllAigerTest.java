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

import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.sts.aiger.AigerParser2;
import hu.bme.mit.theta.sts.aiger.AigerToSts;
import hu.bme.mit.theta.sts.aiger.elements.AigerSystem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class RLiveWithAllAigerTest {

    private static final Map<String, String> TEST_TO_EXPECTED_RESULT = new LinkedHashMap<String, String>() {{
        put("arbi0s08bugp03", "unsafe");
        put("arbixs08bugp03", "unsafe");
        put("cucnt3ro", "safe");
        put("cuhanoi4ro", "unsafe");
        put("cujc128fro", "unsafe");
        put("lmcs06counter0", "safe");
        put("lmcs06counter1", "unsafe");
        put("lmcs06dme6p1", "unsafe");
        put("lmcs06dme6p4", "unsafe");
        put("lmcs06mutex0", "safe");
        put("lmcs06mutex1", "unsafe");
        put("lmcs06ring0", "safe");
        put("lmcs06ring1", "unsafe");
        put("lmcs06short0", "safe");
        put("lmcs06short1", "unsafe");
        put("lmcs06srg5p0", "safe");
        put("lmcs06srg5p1", "unsafe");
        put("lmcs06srg5p2", "unsafe");
    }};

    public static Map<String, String> createBenchmarkMap() {
        Map<String, String> benchmarkStatus = new HashMap<>();

//        benchmarkStatus.put("6s201j18", "safe");
//        benchmarkStatus.put("6s201j20", "safe");
//        benchmarkStatus.put("6s201j34", "safe");
//        benchmarkStatus.put("6s201j36", "safe");
//        benchmarkStatus.put("6s201j38", "safe");
//        benchmarkStatus.put("6s201j40", "safe");
//        benchmarkStatus.put("6s202j01", "unsafe");
//        benchmarkStatus.put("6s202j04", "unsafe");
//        benchmarkStatus.put("6s202j17", "unsafe");
//        benchmarkStatus.put("6s202j30", "unsafe");
//        benchmarkStatus.put("6s202j40", "unsafe");
//        benchmarkStatus.put("6s202j71", "unsafe");
//        benchmarkStatus.put("6s202j79", "unsafe");
//        benchmarkStatus.put("6s203j04", "unsafe");
//        benchmarkStatus.put("6s203j14", "unsafe");
//        benchmarkStatus.put("6s203j17", "unsafe");
//        benchmarkStatus.put("6s203j20", "unsafe");
//        benchmarkStatus.put("6s203j30", "unsafe");
//        benchmarkStatus.put("6s203j57", "unsafe");
//        benchmarkStatus.put("6s203j63", "unsafe");
//        benchmarkStatus.put("6s205j00", "unsafe");
//        benchmarkStatus.put("6s205j21", "unsafe");
//        benchmarkStatus.put("6s205j23", "unsafe");
//        benchmarkStatus.put("6s205j30", "unsafe");
//        benchmarkStatus.put("6s205j34", "unsafe");
//        benchmarkStatus.put("6s205j39", "unsafe");
//        benchmarkStatus.put("6s205j57", "unsafe");
//        benchmarkStatus.put("6s206j000", "safe");
//        benchmarkStatus.put("6s208j1", "unsafe");
//        benchmarkStatus.put("6s208j2", "unsafe");
//        benchmarkStatus.put("6s208j3", "unsafe");
//        benchmarkStatus.put("6s208j4", "unsafe");
//        benchmarkStatus.put("6s208j5", "unsafe");
//        benchmarkStatus.put("6s210j006", "safe");
//        benchmarkStatus.put("6s210j039", "safe");
//        benchmarkStatus.put("6s210j052", "safe");
//        benchmarkStatus.put("6s210j065", "safe");
//        benchmarkStatus.put("6s210j090", "safe");
//        benchmarkStatus.put("6s210j093", "safe");
//        benchmarkStatus.put("6s210j116", "safe");
//        benchmarkStatus.put("6s211", "unsafe");
//        benchmarkStatus.put("6s212", "safe");
//        benchmarkStatus.put("6s213j000", "safe");
//        benchmarkStatus.put("6s213j001", "safe");
//        benchmarkStatus.put("6s213j002", "safe");
//        benchmarkStatus.put("6s214j0", "unsafe");
//        benchmarkStatus.put("6s214j1", "unsafe");
//        benchmarkStatus.put("6s214j2", "unsafe");
//        benchmarkStatus.put("6s215j0", "unsafe");
//        benchmarkStatus.put("6s217j0", "safe");
//        benchmarkStatus.put("6s217j1", "safe");
//        benchmarkStatus.put("6s220", "safe");
//        benchmarkStatus.put("6s221j00", "unsafe");
//        benchmarkStatus.put("6s307j00", "unsafe");
//        benchmarkStatus.put("6s364j00000", "unsafe");
//        benchmarkStatus.put("6s364j00001", "safe");
//        benchmarkStatus.put("6s364j00002", "safe");
//        benchmarkStatus.put("6s364j00003", "safe");
//        benchmarkStatus.put("6s364j00005", "safe");
//        benchmarkStatus.put("arbi0s08bugp03", "unsafe");
//        benchmarkStatus.put("arbi0s08p03", "safe");
//        benchmarkStatus.put("arbi0s16bugp03", "unsafe");
//        benchmarkStatus.put("arbi0s32bugp03", "unsafe");
//        benchmarkStatus.put("arbi0s64bugp03", "unsafe");
//        benchmarkStatus.put("arbixs08bugp03", "unsafe");
//        benchmarkStatus.put("arbixs08p03", "safe");
//        benchmarkStatus.put("arbixs16bugp03", "unsafe");
//        benchmarkStatus.put("arbixs32bugp03", "unsafe");
//        benchmarkStatus.put("cuabq2fro", "safe");
//        benchmarkStatus.put("cuabq2mfro", "safe");
//        benchmarkStatus.put("cuabq4fro", "safe");
//        benchmarkStatus.put("cuabq4mfro", "safe");
//        benchmarkStatus.put("cuabq8fro", "safe");
//        benchmarkStatus.put("cuabq8mfro", "safe");
//        benchmarkStatus.put("cuads11", "unsafe");
//        benchmarkStatus.put("cuads12", "unsafe");
//        benchmarkStatus.put("cuads13", "unsafe");
//        benchmarkStatus.put("cuasq10", "unsafe");
//        benchmarkStatus.put("cuasq11", "unsafe");
//        benchmarkStatus.put("cuasq12", "unsafe");
//        benchmarkStatus.put("cubakro", "unsafe");
        benchmarkStatus.put("cucab09", "safe");
//        benchmarkStatus.put("cucab10", "unsafe");
//        benchmarkStatus.put("cucab11", "safe");
//        benchmarkStatus.put("cucab12", "safe");
//        benchmarkStatus.put("cucab13", "safe");
//        benchmarkStatus.put("cucnt10ro", "safe");
//        benchmarkStatus.put("cucnt128ro", "safe");
//        benchmarkStatus.put("cucnt12ro", "safe");
//        benchmarkStatus.put("cucnt32ro", "safe");
//        benchmarkStatus.put("cucnt3ro", "safe");
//        benchmarkStatus.put("cuffl09", "safe");
//        benchmarkStatus.put("cuffl10", "safe");
//        benchmarkStatus.put("cuffl11", "safe");
//        benchmarkStatus.put("cuffl12", "unsafe");
//        benchmarkStatus.put("cufq1ro", "safe");
//        benchmarkStatus.put("cufq2ro", "unsafe");
//        benchmarkStatus.put("cugbakro", "safe");
//        benchmarkStatus.put("cugcdro", "safe");
//        benchmarkStatus.put("cuhanoi10ro", "unsafe");
//        benchmarkStatus.put("cuhanoi4ro", "unsafe");
//        benchmarkStatus.put("cuhanoi7ro", "unsafe");
//        benchmarkStatus.put("cujc128fro", "unsafe");
//        benchmarkStatus.put("cujc128ro", "safe");
//        benchmarkStatus.put("cujc12ro", "safe");
//        benchmarkStatus.put("cujc32ro", "safe");
//        benchmarkStatus.put("culockro", "safe");
//        benchmarkStatus.put("cunim1ro", "safe");
//        benchmarkStatus.put("cunim2ro", "safe");
//        benchmarkStatus.put("cunim3ro", "unsafe");
//        benchmarkStatus.put("cuom1ro", "safe");
//        benchmarkStatus.put("cuom2ro", "safe");
//        benchmarkStatus.put("cuom3ro", "safe");
//        benchmarkStatus.put("cupts14", "unsafe");
//        benchmarkStatus.put("cupts15", "unsafe");
//        benchmarkStatus.put("cupts16", "unsafe");
//        benchmarkStatus.put("cusarb16ro", "safe");
//        benchmarkStatus.put("cusarb32ro", "safe");
//        benchmarkStatus.put("cutarb16ro", "safe");
//        benchmarkStatus.put("cutarb32ro", "safe");
//        benchmarkStatus.put("cutarb4ro", "safe");
//        benchmarkStatus.put("cutarb8ro", "safe");
//        benchmarkStatus.put("cutf1ro", "safe");
//        benchmarkStatus.put("cutf2ro", "unsafe");
//        benchmarkStatus.put("cutf3ro", "safe");
//        benchmarkStatus.put("cutq1ro", "safe");
//        benchmarkStatus.put("cutq2ro", "unsafe");
//        benchmarkStatus.put("lmcs06abp4p0", "unsafe");
//        benchmarkStatus.put("lmcs06abp4p1", "safe");
//        benchmarkStatus.put("lmcs06abp4p2", "safe");
//        benchmarkStatus.put("lmcs06abp4p3", "unsafe");
//        benchmarkStatus.put("lmcs06abp4p4", "safe");
//        benchmarkStatus.put("lmcs06bc57sp0", "unsafe");
//        benchmarkStatus.put("lmcs06bc57sp1", "safe");
//        benchmarkStatus.put("lmcs06bc57sp2", "safe");
//        benchmarkStatus.put("lmcs06bc57sp3", "safe");
//        benchmarkStatus.put("lmcs06bc57sp4", "unsafe");
//        benchmarkStatus.put("lmcs06bc57sp5", "unsafe");
//        benchmarkStatus.put("lmcs06bc57sp6", "unsafe");
//        benchmarkStatus.put("lmcs06brp0", "safe");
//        benchmarkStatus.put("lmcs06brp1", "unsafe");
//        benchmarkStatus.put("lmcs06brp2", "safe");
//        benchmarkStatus.put("lmcs06brp3", "unsafe");
//        benchmarkStatus.put("lmcs06brp4", "unsafe");
//        benchmarkStatus.put("lmcs06counter0", "safe");
//        benchmarkStatus.put("lmcs06counter1", "unsafe");
//        benchmarkStatus.put("lmcs06dme2p0", "unsafe");
//        benchmarkStatus.put("lmcs06dme2p1", "unsafe");
//        benchmarkStatus.put("lmcs06dme2p2", "unsafe");
//        benchmarkStatus.put("lmcs06dme3p0", "unsafe");
//        benchmarkStatus.put("lmcs06dme3p1", "unsafe");
//        benchmarkStatus.put("lmcs06dme3p3", "unsafe");
//        benchmarkStatus.put("lmcs06dme3p4", "unsafe");
//        benchmarkStatus.put("lmcs06dme4p0", "unsafe");
//        benchmarkStatus.put("lmcs06dme4p1", "unsafe");
//        benchmarkStatus.put("lmcs06dme4p3", "unsafe");
//        benchmarkStatus.put("lmcs06dme4p4", "unsafe");
//        benchmarkStatus.put("lmcs06dme5p0", "unsafe");
//        benchmarkStatus.put("lmcs06dme5p1", "unsafe");
//        benchmarkStatus.put("lmcs06dme5p3", "unsafe");
//        benchmarkStatus.put("lmcs06dme5p4", "unsafe");
//        benchmarkStatus.put("lmcs06dme6p0", "unsafe");
//        benchmarkStatus.put("lmcs06dme6p1", "unsafe");
//        benchmarkStatus.put("lmcs06dme6p3", "unsafe");
//        benchmarkStatus.put("lmcs06dme6p4", "unsafe");
//        benchmarkStatus.put("lmcs06mutex0", "safe");
//        benchmarkStatus.put("lmcs06mutex1", "unsafe");
//        benchmarkStatus.put("lmcs06prodcell0", "unsafe");
//        benchmarkStatus.put("lmcs06prodcell1", "unsafe");
//        benchmarkStatus.put("lmcs06prodcell2", "safe");
//        benchmarkStatus.put("lmcs06prodcell3", "safe");
//        benchmarkStatus.put("lmcs06prodcell4", "safe");
//        benchmarkStatus.put("lmcs06prodcell5", "safe");
//        benchmarkStatus.put("lmcs06prodcell6", "safe");
//        benchmarkStatus.put("lmcs06prodcell7", "unsafe");
//        benchmarkStatus.put("lmcs06prodcell8", "unsafe");
//        benchmarkStatus.put("lmcs06prodcell9", "unsafe");
//        benchmarkStatus.put("lmcs06ring0", "safe");
//        benchmarkStatus.put("lmcs06ring1", "unsafe");
//        benchmarkStatus.put("lmcs06short0", "safe");
//        benchmarkStatus.put("lmcs06short1", "unsafe");
//        benchmarkStatus.put("lmcs06srg5p0", "safe");
//        benchmarkStatus.put("lmcs06srg5p1", "unsafe");
//        benchmarkStatus.put("lmcs06srg5p2", "unsafe");

        return benchmarkStatus;
    }

    @Parameterized.Parameter(0)
    public String aagFilePath;

    @Parameterized.Parameter(1)
    public String expectedResult;

    @Parameterized.Parameters(name = "{index}: {0} -> {1}")
    public static Collection<Object[]> aagFiles() {
        File inputAigsDir = new File("C:\\Users\\mzalu\\Documents\\GitHub\\theta\\aags");
        if (!inputAigsDir.exists() || !inputAigsDir.isDirectory()) {
            throw new RuntimeException("Directory not found: " + inputAigsDir.getAbsolutePath());
        }


        List<Object[]> params = new ArrayList<>();
        File[] files = inputAigsDir.listFiles((dir, name) -> name.endsWith(".aag"));

        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

                var map = createBenchmarkMap();

                // Only add files that are in the map
                if (map.containsKey(baseName)) {
                    params.add(new Object[]{file.getAbsolutePath(), map.get(baseName)});
                }
            }
        }

        return params;
    }

    @Test
    public void testRlivewithAiger() throws Exception {
        System.out.println("Testing file: " + aagFilePath);

        try {
            final AigerSystem aigerSys = AigerParser2.parse(aagFilePath);
            var sts = AigerToSts.createLivenessSts(aigerSys, 0);


            var rLiveChecker = new RLiveChecker<ExplPrec>(sts, new TempChecker<>(), true);

            var result = rLiveChecker.check();
            String actualResult = result.isSafe() ? "safe" : "unsafe";



            assertEquals("file: " + new File(aagFilePath).getName(),
                         expectedResult, actualResult);
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("fairness")) {
                System.out.println("Skipping test: " + e.getMessage());
                org.junit.Assume.assumeTrue("Skipping test with multiple fairness expressions", false);
            } else {
                throw e;
            }
        }
    }
}
