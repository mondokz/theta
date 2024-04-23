package hu.bme.mit.theta.xsts.analysis;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.l2s.LtsTransform;
import hu.bme.mit.theta.core.stmt.Stmt;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.StmtUtils;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

public class XstsL2S<P extends Prec,S extends ExprState> implements LTS<XstsState<S>, XstsAction>, InitFunc<State, P>,ExtendAction {

    private final Expr<BoolType> prop;
    XstsLts<S> baseLts;

    public XstsL2S(XstsLts<S> baseLts, Expr<BoolType> prop) {
        this.prop = prop;
        checkNotNull(baseLts);
        this.baseLts = baseLts;
    }

    @Override
    public Collection<? extends State> getInitStates(P prec) {
        return null;
    }

    @Override
    public Collection<XstsAction> getEnabledActionsFor(XstsState<S> state) {
        baseLts.getEnabledActionsFor(state).stream().map(
                (XstsAction baseAction) -> {
                    var initExpr = And(StmtUtils.toExpr(baseLts.init,VarIndexingFactory.indexing(0)).getExprs());
                    LtsTransform transform = new LtsTransform(new MonolithicExpr(initExpr,baseAction.toExpr(),prop, VarIndexingFactory.indexing(1)));
                    return new XstsAction.create(transform.getTransFunc());
                }
        )
    }

    @Override
    public Stmt extend(Stmt stmt) {
        return null;
    }
}
