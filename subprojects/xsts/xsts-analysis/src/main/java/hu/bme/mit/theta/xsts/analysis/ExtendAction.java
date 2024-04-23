package hu.bme.mit.theta.xsts.analysis;

import hu.bme.mit.theta.core.stmt.Stmt;

public interface ExtendAction {
    public Stmt extend(Stmt stmt);
}
