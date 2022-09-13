package hu.bme.mit.theta.gamma.frontend.dsl;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.SequenceStmt;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.gamma.frontend.dsl.gen.GammaBaseVisitor;
import hu.bme.mit.theta.gamma.frontend.dsl.gen.GammaParser;
import hu.bme.mit.theta.xcfa.model.*;
import hu.bme.mit.theta.xcfa.passes.GammaPasses;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static hu.bme.mit.theta.core.decl.Decls.Var;
import static hu.bme.mit.theta.core.stmt.Stmts.Assume;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;

public class StatechartParserVisitor extends GammaBaseVisitor<List<XcfaLocation>> {

    public StatechartParserVisitor() {
        builder = new XcfaProcedureBuilder("gamma", new GammaPasses());
        builder.createFinalLoc();
    }

    public XcfaProcedureBuilder getBuilder() {
        for (GammaParser.RuleTransitionContext transitionContext : transitionContexts) {
            handleRuleTransition(transitionContext);
        }
        return builder;
    }

    private final List<GammaParser.RuleTransitionContext> transitionContexts = new ArrayList<>();

    private final XcfaProcedureBuilder builder;

    private final Map<String, XcfaLocation> inLocationLut = new LinkedHashMap<>();

    private final Map<String, List<XcfaLocation>> outLocationLut = new LinkedHashMap<>();

    private final Map<String, VarDecl<?>> varLut = new LinkedHashMap<>();

    @Override
    public List<XcfaLocation> visitRuleSynchronousStatechartDefinition(GammaParser.RuleSynchronousStatechartDefinitionContext ctx) {
        return super.visitRuleSynchronousStatechartDefinition(ctx);
    }

    @Override
    public List<XcfaLocation> visitRulePort(GammaParser.RulePortContext ctx) {
        return super.visitRulePort(ctx);
    }

    @Override
    public List<XcfaLocation> visitRuleVariableDeclaration(GammaParser.RuleVariableDeclarationContext ctx) {
        String name = ctx.RULE_ID().getText();
        if (ctx.ruleType().getText().equalsIgnoreCase("integer")) {
            varLut.put(name, Var(name, Int()));
        } else if (ctx.ruleType().getText().equalsIgnoreCase("boolean")) {
            varLut.put(name, Var(name, Bool()));
        } else {
            throw new UnsupportedOperationException();
        }
        return super.visitRuleVariableDeclaration(ctx);
    }

    @Override
    public List<XcfaLocation> visitRuleRegion(GammaParser.RuleRegionContext ctx) {
        List<XcfaLocation> subStates = new ArrayList<>();
        for (GammaParser.RuleStateNodeContext ruleStateContext : ctx.ruleStateNode()) {
            List<XcfaLocation> locations = ruleStateContext.accept(this);
            if (locations != null) {
                subStates.addAll(locations);
            }
        }
        return subStates;
    }

    @Override
    public List<XcfaLocation> visitRuleState(GammaParser.RuleStateContext ctx) {
        List<XcfaLocation> subStates = new ArrayList<>();
        for (GammaParser.RuleRegionContext ruleRegionContext : ctx.ruleRegion()) {
            List<XcfaLocation> locations = ruleRegionContext.accept(this);
            subStates.addAll(locations);
        }

        String name = ctx.RULE_ID().getText();
        if (subStates.size() == 0) {
            XcfaLocation xcfaLocation = new XcfaLocation(name);
            inLocationLut.put(name, xcfaLocation);
            outLocationLut.putIfAbsent(name, new ArrayList<>());
            outLocationLut.get(name).add(xcfaLocation);
            builder.addLoc(xcfaLocation);
            subStates.add(xcfaLocation);
        } else {
            //TODO: first state should be the initial state
            inLocationLut.put(name, subStates.get(0));
            outLocationLut.putIfAbsent(name, new ArrayList<>());
            outLocationLut.get(name).addAll(subStates);
        }
        return subStates;
    }

    @Override
    public List<XcfaLocation> visitRuleInitialState(GammaParser.RuleInitialStateContext ctx) {
        String name = ctx.RULE_ID().getText();
        builder.createInitLoc();
        XcfaLocation xcfaLocation = builder.getInitLoc();
        inLocationLut.put(name, xcfaLocation);
        outLocationLut.putIfAbsent(name, new ArrayList<>());
        outLocationLut.get(name).add(xcfaLocation);
        return super.visitRuleInitialState(ctx);
    }

    @Override
    public List<XcfaLocation> visitRuleTransition(GammaParser.RuleTransitionContext ctx) {
        transitionContexts.add(ctx);
        return super.visitRuleTransition(ctx);
    }

    private XcfaLocation handleRuleTransition(GammaParser.RuleTransitionContext ctx) {
        String from = ctx.RULE_ID(0).getText();
        List<XcfaLocation> locationFrom = outLocationLut.get(from);

        String to = ctx.RULE_ID(1).getText();
        XcfaLocation locationTo = inLocationLut.get(to);

        EventParserVisitor eventParserVisitor = new EventParserVisitor();
        //String eventName = checkNotNull(ctx.ruleTrigger().accept(eventParserVisitor));

        ExpressionParserVisitor expressionParserVisitor = new ExpressionParserVisitor(varLut);
        StatementParserVisitor statementParserVisitor = new StatementParserVisitor(varLut);
        List<XcfaLabel> actionList = new ArrayList<>();
        if (ctx.ruleExpression() != null) {
            Expr<?> guard = ctx.ruleExpression().accept(expressionParserVisitor);
            actionList.add(new StmtLabel(Assume((Expr<BoolType>) guard)));
        }

        for (GammaParser.RuleActionContext ruleActionContext : ctx.ruleAction()) {
            actionList.add(ruleActionContext.accept(statementParserVisitor));
        }
        for (XcfaLocation xcfaLocation : locationFrom) {
            XcfaEdge edge = new XcfaEdge(xcfaLocation, locationTo, new SequenceLabel(actionList));
            builder.addEdge(edge);
        }

        return null;
    }
}
