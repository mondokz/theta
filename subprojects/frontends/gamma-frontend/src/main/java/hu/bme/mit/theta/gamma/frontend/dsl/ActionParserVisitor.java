package hu.bme.mit.theta.gamma.frontend.dsl;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.gamma.frontend.dsl.gen.GammaBaseVisitor;
import hu.bme.mit.theta.gamma.frontend.dsl.gen.GammaParser;
import hu.bme.mit.theta.xcfa.model.FenceLabel;
import hu.bme.mit.theta.xcfa.model.XcfaLabel;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class ActionParserVisitor extends GammaBaseVisitor<List<XcfaLabel>> {
    private final Map<String, VarDecl<?>> varLut;

    public ActionParserVisitor(Map<String, VarDecl<?>> varLut) {
        this.varLut = varLut;
    }

    @Override
    public List<XcfaLabel> visitRuleEntryAction(GammaParser.RuleEntryActionContext ctx) {
        return ctx.ruleAction().accept(this);
    }

    @Override
    public List<XcfaLabel> visitRuleExitAction(GammaParser.RuleExitActionContext ctx) {
        return ctx.ruleAction().accept(this);
    }

    @Override
    public List<XcfaLabel> visitRuleAction(GammaParser.RuleActionContext ctx) {
        return ctx.getChild(0).accept(this);
    }

    @Override
    public List<XcfaLabel> visitRuleBlock(GammaParser.RuleBlockContext ctx) {
        ArrayList<XcfaLabel> labels = new ArrayList<>();
        for (GammaParser.RuleActionContext ruleActionContext : ctx.ruleAction()) {
            labels.addAll(ruleActionContext.accept(this));
        }
        return labels;
    }

    @Override
    public List<XcfaLabel> visitRuleStatement(GammaParser.RuleStatementContext ctx) {
        return ctx.getChild(0).accept(this);
    }

    @Override
    public List<XcfaLabel> visitRuleInlineStatement(GammaParser.RuleInlineStatementContext ctx) {
        ArrayList<XcfaLabel> labels = new ArrayList<>();
        if(ctx.superInlineStatement() != null) {
            labels.addAll(ctx.superInlineStatement().accept(this));
        }

        if(ctx.ruleRaiseEventAction() != null) {
            labels.addAll(ctx.ruleRaiseEventAction().accept(this));
        }
        return labels;
    }

    // TODO multiline statements

    @Override
    public List<XcfaLabel> visitSuperInlineStatement(GammaParser.SuperInlineStatementContext ctx) {
        StatementParserVisitor statementParserVisitor = new StatementParserVisitor(varLut);
        return Collections.singletonList(ctx.getChild(0).accept(statementParserVisitor));
    }

    @Override
    public List<XcfaLabel> visitRuleRaiseEventAction(GammaParser.RuleRaiseEventActionContext ctx) {
        StringBuilder event = new StringBuilder();
        List<TerminalNode> rule_id = ctx.RULE_ID();
        for (int i = 0; i < rule_id.size(); i++) {
            TerminalNode eventNames = rule_id.get(i);
            event.append(eventNames);
            if (i<rule_id.size()-1) event.append(".");
        }
        return Collections.singletonList(new FenceLabel(Collections.singleton(event.toString())));
    }
}
