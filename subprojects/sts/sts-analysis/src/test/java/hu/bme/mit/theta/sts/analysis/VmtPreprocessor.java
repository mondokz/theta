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

import org.junit.platform.commons.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class VmtPreprocessor {
    public static String preprocess(String vmt) {
        String result = vmt;
        Pattern defineFunPattern = Pattern.compile("\\(define-fun\\s+(\\S+)\\s+\\(\\)\\s+((?:\\w+|\\([^)]+\\)))\\s+(\\(.+\\))\\)");

        Matcher matcher = defineFunPattern.matcher(result);
        boolean lastline = false;
        String fullMatch;
        String functionName;
        String body;
        List<String> processedFunctions = new ArrayList<>();
        while (matcher.find() && !lastline) {

            fullMatch = matcher.group(0);
            while (processedFunctions.contains(fullMatch)) {
                matcher.find();
                fullMatch = matcher.group(0);
            }
            processedFunctions.add(fullMatch);
            functionName = matcher.group(1);
            body = matcher.group(3);
            var body2 = balanceParentheses(body);


            boolean initTrue = fullMatch.contains(":init true");
            boolean nextTerm = body.contains(":next");
            boolean keep = fullMatch.contains(":trans") || fullMatch.contains(":invar-property") || initTrue || body.contains("!");
            lastline = fullMatch.contains(":invar-property");
            if (!keep) {
                result = result.replace(fullMatch, "");
            }

            Pattern functionPattern = Pattern.compile( "(?<!define-fun)" + "(\\(|\\s)" + Pattern.quote(functionName) + "(\\)|\\s)");
            if (keep) {
                var x = Pattern.compile("\\(!\\s+(.*)\\s+:.*\\)");
                Matcher m = x.matcher(body);
                if (m.find()) {
                    body = m.group(1);
                    var e = 1;
                }
            }
            final var currentBody = body;
            result = functionPattern.matcher(result).replaceAll(it -> it.group(1) + Matcher.quoteReplacement(currentBody) + it.group(2));

//            result = functionPattern.matcher(result).replaceAll(Matcher.quoteReplacement());

            var x = result;
            matcher = defineFunPattern.matcher(result);
        }
        result = result.replaceAll("(\\r?\\n){2,}", "\n");
        result = result.replaceAll("\\|", "");
        result = result.lines().filter(line -> !line.contains("set-info")).collect(Collectors.joining("\n"));
        return result.replaceAll("#", "");
    }

    public static String balanceParentheses(String input) {
        StringBuilder result = new StringBuilder();
        int openCount = 0;

        for (char c : input.toCharArray()) {
            if (c == '(') {
                openCount++;
                result.append(c);
            } else if (c == ')') {
                if (openCount > 0) {
                    openCount--;
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        result.append(")".repeat(openCount));
        return result.toString();
    }

}
