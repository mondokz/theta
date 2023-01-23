/*
 *  Copyright 2023 Budapest University of Technology and Economics
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

package hu.bme.mit.theta.graphsolver.graphpattern

interface GraphPatternCompiler<T> {
    fun compile(acyclic: hu.bme.mit.theta.graphsolver.graphpattern.constraints.Acyclic): T
    fun compile(cyclic: hu.bme.mit.theta.graphsolver.graphpattern.constraints.Cyclic): T
    fun compile(empty: hu.bme.mit.theta.graphsolver.graphpattern.constraints.Empty): T
    fun compile(nonempty: hu.bme.mit.theta.graphsolver.graphpattern.constraints.Nonempty): T
    fun compile(reflexive: hu.bme.mit.theta.graphsolver.graphpattern.constraints.Reflexive): T
    fun compile(irreflexive: hu.bme.mit.theta.graphsolver.graphpattern.constraints.Irreflexive): T

    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.CartesianProduct): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.Complement): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.ComplementNode): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.Difference): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.DifferenceNode): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.Domain): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.EdgePattern): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.EmptyRelation): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.EmptySet): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.IdentityClosure): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.Intersection): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.IntersectionNode): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.Inverse): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.NodePattern): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.Range): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.ReflexiveTransitiveClosure): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.Self): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.Sequence): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.Toid): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.TransitiveClosure): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.Union): T
    fun compile(pattern: hu.bme.mit.theta.graphsolver.graphpattern.patterns.UnionNode): T
}