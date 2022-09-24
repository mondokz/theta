package hu.bme.mit.theta.gamma.frontend.dsl;

import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.anytype.RefExpr;
import hu.bme.mit.theta.gamma.frontend.dsl.gen.*;

import java.math.BigInteger;
import java.util.Map;

import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Add;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.False;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;

public class ExpressionParserVisitor extends GammaBaseVisitor<Expr<?>> {

    private final Map<String, VarDecl<?>> varLut;

    public ExpressionParserVisitor(Map<String, VarDecl<?>> varLut) {
        this.varLut = varLut;
    }

    @Override
    public Expr<?> visitRuleAdditiveExpression(GammaParser.RuleAdditiveExpressionContext ctx) {
        return Add(
                ctx.ruleMultiplicativeExpression(0).accept(this),
                ctx.ruleMultiplicativeExpression(1).accept(this)
        );
    }

    @Override
    public Expr<?> visitRuleDirectReferenceExpression(GammaParser.RuleDirectReferenceExpressionContext ctx) {
        return varLut.get(ctx.getText()).getRef();
    }

    @Override
    public Expr<?> visitRuleIntegerLiteralExpression(GammaParser.RuleIntegerLiteralExpressionContext ctx) {
        return Int(new BigInteger(ctx.getText()));
    }

    @Override
    public Expr<?> visitRuleFalseExpression(GammaParser.RuleFalseExpressionContext ctx) {
        return True();
    }

    @Override
    public Expr<?> visitRuleTrueExpression(GammaParser.RuleTrueExpressionContext ctx) {
        return False();
    }

    @Override
    public Expr<?> visitRuleAssignableDirectReferenceExpression(GammaParser.RuleAssignableDirectReferenceExpressionContext ctx) {
        if(varLut.containsKey(ctx.getText())) {
            return RefExpr.of(varLut.get(ctx.getText()));
        } else throw new RuntimeException("Direct reference to unknown variable: " + ctx.getText());
    }
}
