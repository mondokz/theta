package hu.bme.mit.theta.xsts.analysis;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.expl.ExplInitFunc;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.common.container.Containers;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.StmtUtils;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;

import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Eq;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;

import java.util.*;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Not;

public class XstsL2S<P extends Prec,S extends ExprState> implements LTS<XstsState<S>, XstsAction>, InitFunc<State, P> {

    public static void main(String[] args) {
        var l2s = new XstsL2S<>(
                null,
                null,
                (expr) -> XstsInitFunc.create(ExplInitFunc.create(null, expr)),
                null,
                null
        );
    }

    private final Expr<BoolType> prop;
    private final Collection<VarDecl<?>> vars;
    private final Function<Expr<BoolType>, ? extends InitFunc<XstsState<S>, ? super P>> initFuncSupplier;
    private final InitFunc<XstsState<S>, ? super P> initFunc;
    XstsLts<S> baseLts;
    VarDecl<BoolType> saved = Decls.Var("saved",BoolType.getInstance());

    public static <P extends Prec,S extends ExprState> XstsL2S<P, S> create(
            XstsLts<S> baseLts, Expr<BoolType> prop,
            Function<Expr<BoolType>, ? extends InitFunc<XstsState<S>, ? super P>> initFuncSupplier,
                                            Expr<BoolType> initExpr,Collection<VarDecl<?>> vars) {
        return new XstsL2S<>(baseLts, prop, initFuncSupplier, initExpr, vars);

    }

    public XstsL2S(
            XstsLts<S> baseLts, Expr<BoolType> prop,
            Function<Expr<BoolType>, ? extends InitFunc<XstsState<S>, ? super P>> initFuncSupplier,
            Expr<BoolType> initExpr, Collection<VarDecl<?>> vars
    ) {
        this.initFuncSupplier = initFuncSupplier;
        var newInitExpr = And(initExpr,Not(saved.getRef()));
        this.initFunc = initFuncSupplier.apply(newInitExpr);
        checkNotNull(baseLts);
        this.baseLts = baseLts;
        this.vars = vars;
        this.prop = extendProp(prop);
    }

    @Override
    public Collection<? extends State> getInitStates(P prec) {
        return initFunc.getInitStates(prec);
    }

    @Override
    public Collection<XstsAction> getEnabledActionsFor(XstsState<S> state) {
        return baseLts.getEnabledActionsFor(state).stream().map(
                (XstsAction baseAction) -> {
                    var xtendr = new XstsActionExtender(vars);
                    return xtendr.extend(baseAction);
                }
        ).toList();
    }

    public Expr<BoolType> extendProp(Expr<BoolType> prop){
        Expr<BoolType> p = True();

        for (var varDecl : vars) {
            var newVar = Decls.Var(varDecl.getName(), varDecl.getType());
            var exp = Eq(newVar.getRef(),varDecl.getRef());
            p = And(p,exp);
        }

        return Not(And(p,prop,saved.getRef()));
    }


}
