package hu.bme.mit.theta.cfa.analysis;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Not;

public class CfaL2S<P extends Prec> implements LTS<CfaState<?>,CfaAction> {

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
        this.prop = prop;
        this.vars = vars;
        this.initFuncSupplier = initFuncSupplier;
        Expr<BoolType> x = True();
        var newInitExpr = And(x,And(initExpr,Not(saved.getRef())));
        this.initFunc = initFuncSupplier.apply(newInitExpr);
        this.xtendr = new CfaActionExtender();
        this.baseLts = baseLts;

    }

    @Override
    public Collection<CfaAction> getEnabledActionsFor(CfaState<?> state) {
        return baseLts.getEnabledActionsFor(state).stream().map(
                (CfaAction baseaction) -> xtendr.extend(baseaction,stmts)
        ).toList();
    }
}
