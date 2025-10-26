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
package hu.bme.mit.theta.sts.aiger.elements;

import java.util.Collections;
import java.util.List;

/**
 * Represents an AIGER system, which is a collection of @link {@link AigerNode}s (connected with
 *
 * <p>{@link AigerWire}s) and an {@link OutputVar}.
 */
public class AigerSystem {

    private final List<AigerNode> nodes;
    private final OutputVar output;
    private final List<JusticeProperty> justiceProperties;
    private final List<Integer> constraints;
    private final List<Integer> fairness;

    public AigerSystem(final List<AigerNode> nodes, final OutputVar output, List<Integer> constraints, List<Integer> fairness) {
        this.nodes = nodes;
        this.output = output;
        this.constraints = constraints;
        this.fairness = fairness;
        this.justiceProperties = Collections.emptyList();
    }

    public AigerSystem(final List<AigerNode> nodes, final OutputVar output,
                       final List<JusticeProperty> justiceProperties, List<Integer> constraints, List<Integer> fairness) {
        this.nodes = nodes;
        this.output = output;
        this.justiceProperties = justiceProperties;
        this.constraints = constraints;
        this.fairness = fairness;
    }

    public List<AigerNode> getNodes() {
        return nodes;
    }

    public OutputVar getOutput() {
        return output;
    }

    public List<JusticeProperty> getJusticeProperties() {
        return justiceProperties;
    }

    public List<Integer> getConstraints() {
        return constraints;
    }

    public List<Integer> getFairness() {
        return fairness;
    }
}