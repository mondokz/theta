package hu.bme.mit.theta.gamma.frontend.dsl;

import hu.bme.mit.theta.gamma.frontend.dsl.gen.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

public class EventParserVisitor extends GammaBaseVisitor<String> {
    @Override
    public String visitRulePortEventReference(GammaParser.RulePortEventReferenceContext ctx) {
        StringBuilder event = new StringBuilder();
        List<TerminalNode> rule_id = ctx.RULE_ID();
        for (int i = 0; i < rule_id.size(); i++) {
            TerminalNode terminalNode = rule_id.get(i);
            event.append(terminalNode.getText());
            if(i!=rule_id.size()-1) {
                event.append(".");
            }
        }

        return event.toString();
    }
}
