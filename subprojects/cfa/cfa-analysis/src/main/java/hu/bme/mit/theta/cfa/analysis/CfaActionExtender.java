package hu.bme.mit.theta.cfa.analysis;

import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.analysis.l2s.ExtendAction;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.core.stmt.SequenceStmt;
import hu.bme.mit.theta.core.stmt.Stmt;

import java.util.ArrayList;
import java.util.List;

public class CfaActionExtender implements ExtendAction<CfaAction> {
    @Override
    public CfaAction extend(CfaAction action, Stmt stmt) {
        // TODO (optional) no phantom edge
        var edge = action.getEdges().stream().findFirst().orElseThrow();
        var newStmt = SequenceStmt.of(List.of(stmt,edge.getStmt()));
        var newEdge = CFA.builder().createEdge(edge.getSource(), edge.getTarget(),newStmt);
        return CfaAction.create(newEdge);
    }
}
