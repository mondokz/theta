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
package hu.bme.mit.theta.sts.analysis;

import static org.junit.Assert.assertTrue;

import hu.bme.mit.theta.sts.STS;
import hu.bme.mit.theta.sts.vmt.VmtToStsConverter;
import org.junit.Test;

public class VmtToStsConverterTest {

    @Test
    public void parseSampleVmtToSts() {
        final String vmt = """
                ; this is a comment
                (declare-const x Int)
                (declare-const x.next Int)
                (define-fun sv.x () Int (! x :next x.next))
                (declare-const b Bool)
                (define-fun init () Bool (! (= x 1) :init true))
                (define-fun trans () Bool
                   (! (= x.next (ite b (+ x 1) x)) :trans true))
                (define-fun p1 () Bool (! (> x 0) :invar-property 1))
                (define-fun p2 () Bool (! (> x 10) :live-property 2))
                """;

        final STS sts = VmtToStsConverter.parseToSts(vmt);
        final String printed = sts.toString();
        System.out.println(printed);

        assertTrue(printed.contains("(init (= x 1))"));
        assertTrue(printed.contains("(trans (= (prime x) (ite b (+ x 1) x)))"));
        assertTrue(printed.contains("(prop (> x 0))"));
    }
}


