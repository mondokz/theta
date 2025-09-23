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

package hu.bme.mit.theta.solver.smtlib.impl.generic;

import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2BaseVisitor;
import hu.bme.mit.theta.solver.smtlib.dsl.gen.SMTLIBv2Parser;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.solver.smtlib.solver.model.SmtLibModel;

import java.util.*;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool;
import static hu.bme.mit.theta.core.decl.Decls.Const;
import static hu.bme.mit.theta.core.utils.TypeUtils.cast;
import hu.bme.mit.theta.solver.impl.StackImpl;

public class VmtSpecificationVisitor extends SMTLIBv2BaseVisitor<Void> {


    private final Map<String, String> stateVariables = new HashMap<>();

    //not annotated with :next
    private final Set<String> inputVariables = new HashSet<>();


    private final Set<String> allVariables = new HashSet<>();


    private final List<Expr<BoolType>> initConditions = new ArrayList<>();


    private final List<Expr<BoolType>> transitionRelations = new ArrayList<>();


    private final Map<Integer, Expr<BoolType>> invariantProperties = new HashMap<>();


    private final Map<Integer, Expr<BoolType>> livenessProperties = new HashMap<>();

    private final GenericSmtLibSymbolTable symbolTable;
    private final GenericSmtLibTermTransformer termTransformer;

    public VmtSpecificationVisitor(GenericSmtLibSymbolTable symbolTable,
                                   GenericSmtLibTermTransformer termTransformer) {
        this.symbolTable = symbolTable;
        this.termTransformer = termTransformer;
    }

    @Override
    public Void visitVmt_specification(SMTLIBv2Parser.Vmt_specificationContext ctx) {

        for (final var cdecl : ctx.const_declaration()) {
            visitConst_declaration(cdecl);
        }


        for (final var fdef : ctx.fun_definition()) {
            visitFun_definition(fdef);
        }

        calcInputVariables();

        return null;
    }

    @Override
    public Void visitConst_declaration(SMTLIBv2Parser.Const_declarationContext ctx) {
        final String varName = ctx.identifier().getText();
        allVariables.add(varName);
        final Type sort = termTransformer.transformSort(ctx.sort());
        symbolTable.put(
                Const(varName, sort),
                varName,
                "(declare-const " + varName + " " + sort + ")"
        );

        return null;
    }

    @Override
    public Void visitFun_definition(SMTLIBv2Parser.Fun_definitionContext ctx) {
        final var funDef = ctx.function_def();

        final var termCtx = funDef.term();
        final var ann = termCtx.annotate_term();
        if (ann != null) {
            processAnnotatedTerm(ann);
        }
        return null;
    }

    private void processAnnotatedTerm(SMTLIBv2Parser.Annotate_termContext ctx) {
        final var baseTerm = ctx.term();
        final var attributes = ctx.attribute();

        for (final var attr : attributes) {
            final var keyword = attr.keyword();
            if (keyword.predefKeyword() == null) continue;

            final var keywordText = keyword.predefKeyword().getText();
            switch (keywordText) {
                case ":next":
                    processNextAnnotation(baseTerm, attr);
                    break;
                case ":init":
                    processInitAnnotation(baseTerm);
                    break;
                case ":trans":
                    processTransAnnotation(baseTerm);
                    break;
                case ":invar-property":
                    processInvarPropertyAnnotation(baseTerm, attr);
                    break;
                case ":live-property":
                    processLivePropertyAnnotation(baseTerm, attr);
                    break;
                default:

            }
        }
    }

    private void processNextAnnotation(SMTLIBv2Parser.TermContext baseTerm, SMTLIBv2Parser.AttributeContext attr) {
        if (baseTerm.qual_identifier() != null
                && baseTerm.qual_identifier().identifier() != null) {
            final String currentVar =
                    baseTerm.qual_identifier().identifier().getText();
            if (attr.attribute_value() != null && attr.attribute_value().symbol() != null) {
                final String nextVar = attr.attribute_value().symbol().getText();
                stateVariables.put(currentVar, nextVar);
            }
        }
    }

    private void processInitAnnotation(SMTLIBv2Parser.TermContext baseTerm) {
        try {
            final var expr = termTransformer.transformTerm(baseTerm, new SmtLibModel(Collections.emptyMap()), new StackImpl<>());
            final var initExpr = cast(expr, Bool());
            initConditions.add(initExpr);
        } catch (Exception e) {
            System.err.println("Error processing init annotation: " + e.getMessage());
        }
    }

    private void processTransAnnotation(SMTLIBv2Parser.TermContext baseTerm) {
        try {
            final var expr = termTransformer.transformTerm(baseTerm, new SmtLibModel(Collections.emptyMap()), new StackImpl<>());
            final var transExpr = cast(expr, Bool());
            transitionRelations.add(transExpr);
        } catch (Exception e) {
            System.err.println("Error processing trans annotation: " + e.getMessage());
        }
    }

    private void processInvarPropertyAnnotation(
            SMTLIBv2Parser.TermContext baseTerm,
            SMTLIBv2Parser.AttributeContext attr) {
        try {
            final var expr = termTransformer.transformTerm(baseTerm, new SmtLibModel(Collections.emptyMap()), new StackImpl<>());
            final var propExpr = cast(expr, Bool());
            final int propId = extractPropertyId(attr);
            invariantProperties.put(propId, propExpr);
        } catch (Exception e) {
            System.err.println("Error processing invariant property: " + e.getMessage());
        }
    }

    private void processLivePropertyAnnotation(SMTLIBv2Parser.TermContext baseTerm, SMTLIBv2Parser.AttributeContext attr) {
        try {
            final var expr = termTransformer.transformTerm(baseTerm, new SmtLibModel(Collections.emptyMap()), new StackImpl<>());
            final var propExpr = cast(expr, Bool());
            final int propId = extractPropertyId(attr);
            livenessProperties.put(propId, propExpr);
        } catch (Exception e) {
            System.err.println("Error processing liveness property: " + e.getMessage());
        }
    }

    private int extractPropertyId(SMTLIBv2Parser.AttributeContext attr) {
        if (attr.attribute_value() != null && attr.attribute_value().spec_constant() != null) {
            var specConst = attr.attribute_value().spec_constant();
            if (specConst.numeral() != null) {
                return Integer.parseInt(specConst.numeral().getText());
            }
        }
        return 0;
    }

    private void calcInputVariables() {
        for (String var : allVariables) {
            if (!stateVariables.containsKey(var) && !stateVariables.containsValue(var)) {
                inputVariables.add(var);
            }
        }
    }



    public Map<String, String> getStateVariables() {
        return Collections.unmodifiableMap(stateVariables);
    }

    public Set<String> getInputVariables() {
        return Collections.unmodifiableSet(inputVariables);
    }

    public List<Expr<BoolType>> getInitConditions() {
        return Collections.unmodifiableList(initConditions);
    }

    public List<Expr<BoolType>> getTransitionRelations() {
        return Collections.unmodifiableList(transitionRelations);
    }

    public Map<Integer, Expr<BoolType>> getInvariantProperties() {
        return Collections.unmodifiableMap(invariantProperties);
    }

    public Map<Integer, Expr<BoolType>> getLivenessProperties() {
        return Collections.unmodifiableMap(livenessProperties);
    }

}



