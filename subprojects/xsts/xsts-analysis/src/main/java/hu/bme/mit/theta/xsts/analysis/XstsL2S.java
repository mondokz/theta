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
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.StmtUtils;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;

import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;

import java.util.*;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsL2S<P extends Prec,S extends ExprState> implements LTS<XstsState<S>, XstsAction>, InitFunc<State, P> {

    public static void main(String[] args) {
        var l2s = new XstsL2S<>(
                null,
                null,
                (expr) -> XstsInitFunc.create(ExplInitFunc.create(null, expr)),
                null
        );
    }

    private final Expr<BoolType> prop;
    private final Collection<VarDecl<?>> vars;
    private final Function<Expr<BoolType>, ? extends InitFunc<XstsState<S>, ? super P>> initFuncSupplier;
    private final InitFunc<XstsState<S>, ? super P> initFunc;
    XstsLts<S> baseLts;

    public static <P extends Prec,S extends ExprState> XstsL2S<P, S> create(
            XstsLts<S> baseLts, Expr<BoolType> prop,
            Function<Expr<BoolType>, ? extends InitFunc<XstsState<S>, ? super P>> initFuncSupplier,
                                            Expr<BoolType> initExpr) {
        return new XstsL2S<>(baseLts, prop, initFuncSupplier, initExpr);

    }

    public XstsL2S(
            XstsLts<S> baseLts, Expr<BoolType> prop,
            Function<Expr<BoolType>, ? extends InitFunc<XstsState<S>, ? super P>> initFuncSupplier,
            Expr<BoolType> initExpr
    ) {
        this.prop = prop;
        this.initFuncSupplier = initFuncSupplier;
        var newExpr = initExpr;
        this.initFunc = initFuncSupplier.apply(newExpr);
        checkNotNull(baseLts);
        this.baseLts = baseLts;


        final Set<VarDecl<?>> tmpVars = Containers.createSet();
        ExprUtils.collectVars(StmtUtils.toExpr(baseLts.trans,VarIndexingFactory.indexing(0)).getExprs(), tmpVars);
        ExprUtils.collectVars(StmtUtils.toExpr(baseLts.init,VarIndexingFactory.indexing(0)).getExprs(), tmpVars);
        ExprUtils.collectVars(StmtUtils.toExpr(baseLts.env,VarIndexingFactory.indexing(0)).getExprs(), tmpVars);
        ExprUtils.collectVars(prop, tmpVars);
        this.vars = Collections.unmodifiableCollection(tmpVars);
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
                    return XstsAction.create(xtendr.extend(baseAction.getStmts()));
                }
        ).toList();
    }

    public Expr<BoolType> extendInitExpr(Expr<BoolType> init){
        return null;
    }


}
