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

    VarDecl<BoolType> saved;

    Collection<VarDecl<?>> vars;

    XstsActionExtender(Collection<VarDecl<?>> vars){
        this.vars = vars;
        this.saved = Decls.Var("saved",BoolType.getInstance());

    }
    @Override
    public XstsAction extend(StmtAction action) {
        var stmts = action.getStmts();
        ArrayList<Stmt> skip = new ArrayList<>(Collections.singleton(SkipStmt.getInstance()));
        var assignList = new ArrayList<Stmt>();


        for (var varDecl : vars) {
            var newVar = Decls.Var(varDecl.getName(), varDecl.getType());
            assignList.add(AssignStmt.of((VarDecl<Type>)newVar, (Expr<Type>) varDecl.getRef()));
        }

        assignList.add(AssignStmt.of(saved, True()));
        var seq = SequenceStmt.of(assignList);
        skip.add(seq);

        var nonDet = NonDetStmt.of(skip);


        stmts.add(nonDet);
        return XstsAction.create(stmts);
    }
}
