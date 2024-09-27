package hu.bme.mit.theta.xsts.analysis;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.analysis.l2s.ExtendAction;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.*;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;

public class XstsActionExtender implements ExtendAction {


    XstsActionExtender(){


    }
    @Override
    public XstsAction extend(StmtAction action, Stmt stmt) {
        var stmts = new ArrayList<>(action.getStmts());
        stmts.add(stmt);
        return XstsAction.create(stmts);
    }
}
