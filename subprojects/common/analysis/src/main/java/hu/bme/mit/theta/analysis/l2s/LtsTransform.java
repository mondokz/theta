
package hu.bme.mit.theta.analysis.l2s;

import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;

import hu.bme.mit.theta.common.container.Containers;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.*;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.abstracttype.EqExpr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.StmtUtils;
import hu.bme.mit.theta.core.utils.indexings.BasicVarIndexing;
import hu.bme.mit.theta.core.utils.indexings.VarIndexing;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingBuilder;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;



import java.util.*;

import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Eq;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.*;

public class LtsTransform {

    MonolithicExpr monolithicExpr;
    Collection<VarDecl<?>> vars;
    Collection<VarDecl<?>> newVars;
    VarDecl<BoolType> saved;
    VarDecl<BoolType> shouldSave;
    Map<VarDecl<?>, VarDecl<?>> saveMap = new HashMap<>();

    public LtsTransform(MonolithicExpr monolithicExpr){
        this.monolithicExpr = monolithicExpr;
        this.saved = Decls.Var("saved",BoolType.getInstance());
        this.shouldSave = Decls.Var("shouldSave",BoolType.getInstance());

        final Set<VarDecl<?>> tmpVars = Containers.createSet();
        ExprUtils.collectVars(monolithicExpr.getInitExpr(), tmpVars);
        ExprUtils.collectVars(monolithicExpr.getTransExpr(), tmpVars);
        ExprUtils.collectVars(monolithicExpr.getPropExpr(), tmpVars);
        this.vars = Collections.unmodifiableCollection(tmpVars);
        for (var varDecl : vars) {
            var newVar = Decls.Var("_saved_"+varDecl.getName(), varDecl.getType());
            saveMap.put(varDecl, newVar);
        }
    }

    public Expr<BoolType> getInitFunc(){
        return And(monolithicExpr.getInitExpr(),Not(saved.getRef()));
    }
    public Expr<BoolType> getTransFunc(){

        var saveList = new ArrayList<Expr<BoolType>>();
        var skipList = new ArrayList<Expr<BoolType>>();
        newVars = new ArrayList<>();
        var indx = VarIndexingFactory.indexing(1);
        for (var varDecl : saveMap.entrySet()) {
            saveList.add(Eq(ExprUtils.applyPrimes(varDecl.getValue().getRef(),indx), varDecl.getKey().getRef()));
        }
        for (var varDecl : saveMap.values()) {
            skipList.add(Eq(ExprUtils.applyPrimes((varDecl).getRef(),indx), varDecl.getRef()));
        }
        /*
        v1,v2,v3,... -> T: v1'==v1+1
        ____________ AND
        _saved_v1, _saved_v2, ... -> skip or save
         */
        skipList.add(Eq(ExprUtils.applyPrimes(saved.getRef(),indx),saved.getRef()));
        saveList.add(Eq(ExprUtils.applyPrimes(saved.getRef(),indx), True()));
        var skipOrSave = Or(And(skipList),And(saveList));
        var t = new ArrayList<>(Collections.singleton(monolithicExpr.getTransExpr()));
        t.add(skipOrSave);


        return And(t);
    }
    public Expr<BoolType> getProp(){

        Expr<BoolType> prop = saved.getRef();

        for (var varDecl : saveMap.entrySet()) {
            var exp = Eq(varDecl.getValue().getRef(), varDecl.getKey().getRef());
            prop = And(exp,prop);
        }

        return Not(And(prop,monolithicExpr.getPropExpr()));
    }

    public VarIndexing getOffsetIndexing(){
        var newIndexing = monolithicExpr.getOffsetIndex();
        for (var varDecl : saveMap.values()) {
            newIndexing = newIndexing.inc(varDecl,1);
        }
        newIndexing = newIndexing.inc(saved,1);
        return newIndexing;
    }
}