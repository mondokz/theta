package hu.bme.mit.theta.cfa.analysis;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.*;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;

import java.util.*;
import java.util.function.Function;

import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Eq;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Not;

public class CfaL2S<P extends Prec> implements LTS<CfaState<?>,CfaAction>, InitFunc<State,P> {

    private final Expr<BoolType> prop;
    private final Collection<VarDecl<?>> vars;
    private final Function<Expr<BoolType>, ? extends InitFunc<CfaState<?>,P>> initFuncSupplier;
    private final InitFunc<CfaState<?>,P> initFunc;
    private CfaActionExtender xtendr;
    public Map<VarDecl<?>, VarDecl<?>> varMap;
    public Stmt stmts;
    LTS<CfaState<?>, CfaAction> baseLts;
    VarDecl<BoolType> saved = Decls.Var("saved",BoolType.getInstance());

    public CfaL2S(LTS<CfaState<?>, CfaAction> baseLts,
                  Expr<BoolType> prop, Collection<VarDecl<?>> vars,
                  Function<Expr<BoolType>, ? extends InitFunc<CfaState<?>, P>> initFuncSupplier,
                  Expr<BoolType> initExpr) {
        this.prop = extendProp(prop);
        this.vars = vars;
        this.initFuncSupplier = initFuncSupplier;

        this.varMap = new HashMap<>();
        for (var varDecl : vars) {
            var newVar = Decls.Var(varDecl.getName()+"__saved", varDecl.getType());
            varMap.put(varDecl, newVar);
        }

        Expr<BoolType> x = True();
        for (var varDecl : varMap.keySet()) {
            var exp = Eq(varDecl.getRef(),varMap.get(varDecl).getRef());
            x = And(x,exp);
        }
        var newInitExpr = And(x,And(initExpr,Not(saved.getRef())));
        this.initFunc = initFuncSupplier.apply(newInitExpr);
        this.xtendr = new CfaActionExtender();
        this.baseLts = baseLts;
        this.stmts = getStmts();



    }

    @Override
    public Collection<CfaAction> getEnabledActionsFor(CfaState<?> state) {
        return baseLts.getEnabledActionsFor(state).stream().map(
                (CfaAction baseaction) -> xtendr.extend(baseaction,stmts)
        ).toList();
    }

    public Stmt getStmts(){

        ArrayList<Stmt> result = new ArrayList<>(Collections.singleton(SkipStmt.getInstance()));
        var saveList = new ArrayList<Stmt>();

        for (var varDecl : varMap.keySet()) {
            saveList.add(AssignStmt.of((VarDecl<Type>) varMap.get(varDecl), (Expr<Type>) varDecl.getRef()));
        }

        saveList.add(AssignStmt.of(saved, True()));
        var seq = SequenceStmt.of(saveList);
        result.add(seq);


        return NonDetStmt.of(result);
    }

    public Expr<BoolType> extendProp(Expr<BoolType> prop){
        Expr<BoolType> p = True();

        for (var varDecl : varMap.keySet()) {
            var exp = Eq(varMap.get(varDecl).getRef(),varDecl.getRef());
            p = And(p,exp);
        }

        return Not(And(p,prop,saved.getRef()));
    }
    public Collection<VarDecl<?>> getAllVars(){
        var result = new ArrayList<>(varMap.keySet());
        result.addAll(varMap.values());
        result.add(saved);
        return result;
    }

    @Override
    public Collection<? extends State> getInitStates(P prec) {
        return initFunc.getInitStates(prec);
    }
}
