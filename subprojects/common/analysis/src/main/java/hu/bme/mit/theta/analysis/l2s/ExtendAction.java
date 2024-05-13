package hu.bme.mit.theta.analysis.l2s;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.Stmt;

import java.util.Collection;
import java.util.List;

public interface ExtendAction {
    StmtAction extend(StmtAction action);
}
