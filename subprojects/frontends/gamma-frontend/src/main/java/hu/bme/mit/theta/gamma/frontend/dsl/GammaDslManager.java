/*
 *  Copyright 2017 Budapest University of Technology and Economics
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
package hu.bme.mit.theta.gamma.frontend.dsl;

import hu.bme.mit.theta.gamma.frontend.dsl.gen.GammaLexer;
import hu.bme.mit.theta.gamma.frontend.dsl.gen.GammaParser;
import hu.bme.mit.theta.xcfa.model.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class GammaDslManager {

    private GammaDslManager() {
    }

    public static XCFA createCfa(final String inputString) throws IOException {
        final InputStream stream = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));
        return createCfa(stream);
    }

    public static XCFA createCfa(final InputStream inputStream) throws IOException {
        final CharStream input = CharStreams.fromStream(inputStream);

        final GammaLexer lexer = new GammaLexer(input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final GammaParser parser = new GammaParser(tokens);

        final GammaParser.RuleSynchronousStatechartDefinitionContext context = parser.ruleSynchronousStatechartDefinition();
        StatechartParserVisitor statechartParserVisitor = new StatechartParserVisitor();
        context.accept(statechartParserVisitor);
        XcfaBuilder xcfaBuilder = new XcfaBuilder("gamma");

        XcfaProcedureBuilder builder = statechartParserVisitor.getBuilder();
        xcfaBuilder.addProcedure(builder);
        xcfaBuilder.addEntryPoint(builder, List.of());
        return xcfaBuilder.build();
    }

}
