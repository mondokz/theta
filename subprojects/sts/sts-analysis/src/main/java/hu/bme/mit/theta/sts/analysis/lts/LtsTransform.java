
package hu.bme.mit.theta.sts.analysis.lts;

import hu.bme.mit.theta.common.Tuple3;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.*;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.abstracttype.EqExpr;
import hu.bme.mit.theta.core.type.booltype.AndExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.booltype.SmartBoolExprs;
import hu.bme.mit.theta.core.utils.StmtUtils;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;
import hu.bme.mit.theta.sts.STS;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Not;

public class LtsTransform {
    public Tuple3<Expr<BoolType>, Expr<BoolType>, Expr<BoolType>> lts(STS sts) {
        List<VarDecl<?>> savedVarDecls = new ArrayList<>();
        ArrayList<Stmt> skip = new ArrayList<>(Collections.singleton(SkipStmt.getInstance()));
        var assignList = new ArrayList<Stmt>();
        Expr<BoolType> prop = AndExpr.of(null);
        var saved = Decls.Var("saved",BoolType.getInstance());
        var init = And(sts.getInit(),Not(saved.getRef()));
        for (var varDecl : sts.getVars()) {
            VarDecl newVar = Decls.Var(varDecl.getName(), varDecl.getType());

            assignList.add(AssignStmt.of(newVar, (Expr<Type>) varDecl.getRef()));

            var exp = And(EqExpr.create2(newVar.getRef(),varDecl.getRef()),sts.getProp(),saved.getRef());
            prop = And(prop,exp);

        }
        var seq = SequenceStmt.of(assignList);
        skip.add(seq);
        var nonDet = NonDetStmt.of(skip);

        var t = StmtUtils.toExpr(nonDet, VarIndexingFactory.indexing(0)).getExprs();
        t.add(sts.getTrans());
        var tran = AndExpr.of(t);

        return Tuple3.of(init, tran, prop);
    }
}