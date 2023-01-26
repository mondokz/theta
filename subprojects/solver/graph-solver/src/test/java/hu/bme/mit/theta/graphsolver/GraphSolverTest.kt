/*
 *  Copyright 2022 Budapest University of Technology and Economics
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
package hu.bme.mit.theta.graphsolver

import hu.bme.mit.theta.common.Tuple
import hu.bme.mit.theta.common.Tuple2
import hu.bme.mit.theta.graphsolver.compilers.GraphPatternCompiler
import hu.bme.mit.theta.graphsolver.compilers.pattern2expr.Pattern2ExprCompiler
import hu.bme.mit.theta.graphsolver.patterns.constraints.*
import hu.bme.mit.theta.graphsolver.patterns.patterns.BasicRelation
import hu.bme.mit.theta.graphsolver.solvers.GraphSolver
import hu.bme.mit.theta.graphsolver.solvers.SATGraphSolver
import hu.bme.mit.theta.solver.z3.Z3SolverFactory
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.util.*

@RunWith(Parameterized::class)
class GraphSolverTest<T> {
    @Parameterized.Parameter(0)
    @JvmField
    var constraint: GraphConstraint? = null

    @Parameterized.Parameter(1)
    @JvmField
    var compiler: GraphPatternCompiler<T, *>? = null

    @Parameterized.Parameter(2)
    @JvmField
    var solver: GraphSolver<T>? = null

    @Parameterized.Parameter(3)
    @JvmField
    var allowed: Boolean = false

    @Test
    @Throws(IOException::class)
    fun test() {
        val compiledConstraint = constraint!!.accept(compiler!!)
        solver!!.add(compiledConstraint)
        val status = solver!!.check()
        Assert.assertEquals(allowed, status.isSat)
    }

    companion object {

        private val smallLine: Pair<List<Int>, Map<Pair<String, Tuple>, Boolean>> = Pair(listOf(1, 2, 3), mapOf(
                Pair(Pair("po", Tuple2.of(1, 1)), false),
                Pair(Pair("po", Tuple2.of(1, 2)), true),
                Pair(Pair("po", Tuple2.of(1, 3)), false),
                Pair(Pair("po", Tuple2.of(2, 1)), false),
                Pair(Pair("po", Tuple2.of(2, 2)), false),
                Pair(Pair("po", Tuple2.of(2, 3)), true),
                Pair(Pair("po", Tuple2.of(3, 1)), false),
                Pair(Pair("po", Tuple2.of(3, 2)), false),
                Pair(Pair("po", Tuple2.of(3, 3)), false),
        ))

        private val smallCycle: Pair<List<Int>, Map<Pair<String, Tuple>, Boolean>>  = Pair(listOf(1, 2, 3), mapOf(
                Pair(Pair("po", Tuple2.of(1, 1)), false),
                Pair(Pair("po", Tuple2.of(1, 2)), true),
                Pair(Pair("po", Tuple2.of(1, 3)), false),
                Pair(Pair("po", Tuple2.of(2, 1)), false),
                Pair(Pair("po", Tuple2.of(2, 2)), false),
                Pair(Pair("po", Tuple2.of(2, 3)), true),
                Pair(Pair("po", Tuple2.of(3, 1)), true),
                Pair(Pair("po", Tuple2.of(3, 2)), false),
                Pair(Pair("po", Tuple2.of(3, 3)), false),
        ))

        private val smallFull: Pair<List<Int>, Map<Pair<String, Tuple>, Boolean>>  = Pair(listOf(1, 2, 3), mapOf(
                Pair(Pair("po", Tuple2.of(1, 1)), true),
                Pair(Pair("po", Tuple2.of(1, 2)), true),
                Pair(Pair("po", Tuple2.of(1, 3)), true),
                Pair(Pair("po", Tuple2.of(2, 1)), true),
                Pair(Pair("po", Tuple2.of(2, 2)), true),
                Pair(Pair("po", Tuple2.of(2, 3)), true),
                Pair(Pair("po", Tuple2.of(3, 1)), true),
                Pair(Pair("po", Tuple2.of(3, 2)), true),
                Pair(Pair("po", Tuple2.of(3, 3)), true),
        ))


        @Parameterized.Parameters
        @JvmStatic
        fun data(): Collection<Array<Any>> {
            return Arrays.asList(
                    arrayOf(
                            Acyclic(BasicRelation("po")),
                            Pattern2ExprCompiler(smallLine.first, smallLine.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            true
                    ),
                    arrayOf(
                            Acyclic(BasicRelation("po")),
                            Pattern2ExprCompiler(smallCycle.first, smallCycle.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            false
                    ),
                    arrayOf(
                            Acyclic(BasicRelation("po")),
                            Pattern2ExprCompiler(smallFull.first, smallFull.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            false
                    ),
                    arrayOf(
                            Cyclic(BasicRelation("po")),
                            Pattern2ExprCompiler(smallLine.first, smallLine.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            false
                    ),
                    arrayOf(
                            Cyclic(BasicRelation("po")),
                            Pattern2ExprCompiler(smallCycle.first, smallCycle.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            true
                    ),
                    arrayOf(
                            Cyclic(BasicRelation("po")),
                            Pattern2ExprCompiler(smallFull.first, smallFull.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            true
                    ),
                    arrayOf(
                            Reflexive(BasicRelation("po")),
                            Pattern2ExprCompiler(smallLine.first, smallLine.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            false
                    ),
                    arrayOf(
                            Reflexive(BasicRelation("po")),
                            Pattern2ExprCompiler(smallCycle.first, smallCycle.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            false
                    ),
                    arrayOf(
                            Reflexive(BasicRelation("po")),
                            Pattern2ExprCompiler(smallFull.first, smallFull.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            true
                    ),
                    arrayOf(
                            Irreflexive(BasicRelation("po")),
                            Pattern2ExprCompiler(smallLine.first, smallLine.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            true
                    ),
                    arrayOf(
                            Irreflexive(BasicRelation("po")),
                            Pattern2ExprCompiler(smallCycle.first, smallCycle.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            true
                    ),
                    arrayOf(
                            Irreflexive(BasicRelation("po")),
                            Pattern2ExprCompiler(smallFull.first, smallFull.second),
                            SATGraphSolver(Z3SolverFactory.getInstance().createSolver()),
                            false
                    ),
            )
        }
    }
}