package hu.bme.mit.theta.gamma.frontend.dsl;

import hu.bme.mit.theta.gamma.frontend.dsl.gen.GammaBaseVisitor;
import hu.bme.mit.theta.gamma.frontend.dsl.gen.GammaParser;

public class EventParserVisitor extends GammaBaseVisitor<String> {
    @Override
    public String visitRulePortEventReference(GammaParser.RulePortEventReferenceContext ctx) {
        return ctx.getText();
    }
}
