package hu.bme.mit.theta.sts.analysis;

import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.bounded.MonolithicExpr;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory;
import hu.bme.mit.theta.analysis.l2s.L2STransform;


import java.util.Collection;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class StsL2S<P extends Prec> implements LTS<State, StsAction>, InitFunc<State, P> {
    private final StsLts baseLts;
    private final Expr<BoolType> init;
    private final Expr<BoolType> prop;


    public StsL2S(StsLts baseLts,Expr<BoolType> init, Expr<BoolType> prop) {
        this.init = init;
        this.prop = prop;
        checkNotNull(baseLts);
        this.baseLts = baseLts;
    }

    @Override
    public Collection<? extends State> getInitStates(P prec) {
        return null;
    }

    @Override
    public Collection<StsAction> getEnabledActionsFor(State state) {
        return baseLts.getEnabledActionsFor(state).stream().map(
                (StsAction baseAction) -> {
                    var baseExpr = baseAction.toExpr();
                    L2STransform transform = new L2STransform(new MonolithicExpr(init,baseExpr,prop, VarIndexingFactory.indexing(1)));
                    return new StsAction(transform.getTransFunc());
                }
        ).collect(Collectors.toList());
    }

}

