package hu.bme.mit.theta.xsts.analysis;

import com.google.errorprone.annotations.Var;
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
import hu.bme.mit.theta.core.stmt.*;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
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
    private final Function<Expr<BoolType>, ? extends InitFunc<XstsState<S>,P>> initFuncSupplier;
    private final InitFunc<XstsState<S>,P> initFunc;
    private XstsActionExtender xtendr;
    public Map<VarDecl<?>, VarDecl<?>> varMap;
    public Stmt stmts;
    LTS<XstsState<S>, XstsAction> baseLts;
    VarDecl<BoolType> saved = Decls.Var("saved",BoolType.getInstance());

    public static <P extends Prec,S extends ExprState> XstsL2S<P, S> create(
            LTS<XstsState<S>, XstsAction> baseLts, Expr<BoolType> prop,
            Function<Expr<BoolType>, ? extends InitFunc<XstsState<S>,P>> initFuncSupplier,
                                            Expr<BoolType> initExpr,Collection<VarDecl<?>> vars) {
        return new XstsL2S<>(baseLts, prop, initFuncSupplier, initExpr, vars);

    }

    public XstsL2S(
            LTS<XstsState<S>, XstsAction> baseLts, Expr<BoolType> prop,
            Function<Expr<BoolType>, ? extends InitFunc<XstsState<S>,P>> initFuncSupplier,
            Expr<BoolType> initExpr, Collection<VarDecl<?>> vars
    ) {
        this.varMap = new HashMap<>();
        for (var varDecl : vars) {
            var newVar = Decls.Var(varDecl.getName()+"__saved", varDecl.getType());
            varMap.put(varDecl, newVar);
        }

        this.initFuncSupplier = initFuncSupplier;
        Expr<BoolType> x = True();
        for (var varDecl : varMap.keySet()) {
            var exp = Eq(varDecl.getRef(),varMap.get(varDecl).getRef());
            x = And(x,exp);
        }
        var newInitExpr = And(x,And(initExpr,Not(saved.getRef()))); //todo: and(this, összes mentett var: kezedeti value = eredeti value)
        this.initFunc = initFuncSupplier.apply(newInitExpr);
        checkNotNull(baseLts);
        this.baseLts = baseLts;
        this.vars = vars;
        this.prop = extendProp(prop);
        this.xtendr = new XstsActionExtender();
        this.stmts = getStmts();

    }

    public Collection<VarDecl<?>> getVars() {
        var all = new ArrayList<>(varMap.keySet());
        all.addAll(varMap.values());
        all.add(saved);
        return all;
    }

    @Override
    public Collection<? extends XstsState<S>> getInitStates(P prec) {
        return initFunc.getInitStates(prec);
    }

    public Stmt getStmts(){

        ArrayList<Stmt> result = new ArrayList<>(Collections.singleton(SkipStmt.getInstance()));
        var assignList = new ArrayList<Stmt>();


        for (var varDecl : varMap.keySet()) {
            assignList.add(AssignStmt.of((VarDecl<Type>) varMap.get(varDecl), (Expr<Type>) varDecl.getRef()));
        }

        assignList.add(AssignStmt.of(saved, True()));
        var seq = SequenceStmt.of(assignList);
        result.add(seq);

        return NonDetStmt.of(result);
    }

    @Override
    public Collection<XstsAction> getEnabledActionsFor(XstsState<S> state) {
        // todo: only in target
        return baseLts.getEnabledActionsFor(state).stream().map(
                (XstsAction baseAction) -> xtendr.extend(baseAction,stmts)
        ).toList();
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


}
