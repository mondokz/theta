
package hu.bme.mit.theta.sts.analysis.lts;

import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;

import hu.bme.mit.theta.common.container.Containers;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.*;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.StmtUtils;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;



import java.util.*;

import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Eq;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Not;

public class LtsTransform {

    MonolithicExpr monolithicExpr;
    Collection<VarDecl<?>> vars;
    VarDecl<BoolType> saved;

    public LtsTransform(MonolithicExpr monolithicExpr){
        this.monolithicExpr = monolithicExpr;
        this.saved = Decls.Var("saved",BoolType.getInstance());

        final Set<VarDecl<?>> tmpVars = Containers.createSet();
        ExprUtils.collectVars(monolithicExpr.getInitExpr(), tmpVars);
        ExprUtils.collectVars(monolithicExpr.getTransExpr(), tmpVars);
        ExprUtils.collectVars(monolithicExpr.getPropExpr(), tmpVars);
        this.vars = Collections.unmodifiableCollection(tmpVars);
    }

    public Expr<BoolType> getInitFunc(){
        return And(monolithicExpr.getInitExpr(),Not(saved.getRef()));
    }
    public Expr<BoolType> getTransFunc(){

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
        var saveOrSkip = StmtUtils.toExpr(nonDet, VarIndexingFactory.indexing(0)).getExprs();
        var t = new ArrayList<>(Collections.singleton(monolithicExpr.getTransExpr()));
        t.addAll(saveOrSkip);

        return And(t);
    }
    public Expr<BoolType> getProp(){

        Expr<BoolType> prop = True();

        for (var varDecl : vars) {
            var newVar = Decls.Var(varDecl.getName(), varDecl.getType());
            var exp = Eq(newVar.getRef(),varDecl.getRef());
            prop = And(prop,exp);
        }

        return Not(And(prop,monolithicExpr.getPropExpr(),saved.getRef()));
    }
}