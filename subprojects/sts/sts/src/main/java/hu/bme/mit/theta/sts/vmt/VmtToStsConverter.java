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
package hu.bme.mit.theta.sts.vmt;

import static hu.bme.mit.theta.core.type.anytype.Exprs.Prime;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;

import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.model.BasicSubstitution;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Lexer;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSymbolTable;
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibTermTransformer;
import hu.bme.mit.theta.solver.smtlib.impl.generic.VmtSpecificationVisitor;
import hu.bme.mit.theta.solver.smtlib.solver.parser.ThrowExceptionErrorListener;
import hu.bme.mit.theta.sts.STS;
import java.util.HashMap;
import java.util.Map;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;


public final class VmtToStsConverter {

    private VmtToStsConverter() {}

    public static STS parseToSts(final String vmt) {
        final var lexer = new SMTLIBv2Lexer(CharStreams.fromString(vmt));
        final var parser = new SMTLIBv2Parser(new CommonTokenStream(lexer));
        final var errListener = new ThrowExceptionErrorListener();
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(errListener);
        parser.addErrorListener(errListener);

        final var tree = parser.vmt_specification();
        final var symbolTable = new GenericSmtLibSymbolTable();
        final var termTransformer = new GenericSmtLibTermTransformer(symbolTable);
        final var visitor = new VmtSpecificationVisitor(symbolTable, termTransformer);
        visitor.visit(tree);

        final Expr<BoolType> initExpr =
                visitor.getInitConditions().isEmpty()
                        ? True()
                        : And(visitor.getInitConditions());
        final Expr<BoolType> transExpr =
                visitor.getTransitionRelations().isEmpty()
                        ? True()
                        : And(visitor.getTransitionRelations());
        final Expr<BoolType> propExpr =
                visitor.getInvariantProperties().isEmpty()
                        ? True()
                        : And(visitor.getInvariantProperties().values());

        //  substitution from :next annotations
        final var substBuilder = BasicSubstitution.builder();
        final Map<String, String> currToNext = visitor.getStateVariables();
        for (final var entry : currToNext.entrySet()) {
            final String currName = entry.getKey();
            final String nextName = entry.getValue();
            final var currDecl = symbolTable.getConst(currName);
            final var currVarDecl = Decls.Var(currDecl.getName(), currDecl.getType());
            final var nextDecl = symbolTable.getConst(nextName);

            // substitute .next -> prime and also constdecl -> vardecl
            substBuilder.put(nextDecl, Prime(currVarDecl.getRef()));
            substBuilder.put(currDecl, currVarDecl.getRef());
        }

        final var subst = substBuilder.build();

        final Expr<BoolType> initP = subst.apply(initExpr);
        final Expr<BoolType> transP = subst.apply(transExpr);
        final Expr<BoolType> propP = subst.apply(propExpr);

        return STS.builder().addInit(initP).addTrans(transP).setProp(propP).build();
    }
}
