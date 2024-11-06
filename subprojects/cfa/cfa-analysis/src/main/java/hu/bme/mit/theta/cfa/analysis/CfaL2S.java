package hu.bme.mit.theta.cfa.analysis;

import com.google.common.base.Preconditions;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.core.decl.Decls;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.stmt.*;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.Type;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.type.inttype.IntExprs;
import hu.bme.mit.theta.core.type.inttype.IntType;

import java.util.*;
import java.util.stream.Stream;

import static hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Eq;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.True;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.And;
import static hu.bme.mit.theta.core.type.booltype.SmartBoolExprs.Not;
import static hu.bme.mit.theta.core.type.inttype.IntExprs.Int;

import hu.bme.mit.theta.core.stmt.Stmt;

import java.util.Collection;
import java.util.Map;

public class CfaL2S<S extends ExprState> implements LTS<CfaState<S>, CfaAction>{

    private final VarDecl<IntType> locVar;
    private final Map<CFA.Loc, Integer> map;
    private final Expr<BoolType> initExpr;
    private final CfaActionExtender xtendr = new CfaActionExtender();
    private final CFA cfa;
    static Map<VarDecl<?>, VarDecl<?>> varMap = new HashMap<>();
    private final Stmt saveStmt;
    Expr<BoolType> prop;
    private final Collection<VarDecl<?>> vars;
    LTS<CfaState<?>, CfaAction> baseLts;
    static VarDecl<BoolType> saved = Decls.Var("saved",BoolType.getInstance());


    public static CfaL2S create(final LTS<CfaState<?>, CfaAction> baseLts,
                                final CFA cfa,
                                final Collection<VarDecl<?>> vars)
    {
        final VarDecl<IntType> locVar = Decls.Var("loc", Int());

        int i = 0;
        final Map<CFA.Loc, Integer> map = new HashMap<>();
        for (var x : cfa.getLocs()) {
            map.put(x, i++);
        }

        var tempList = new ArrayList<>(vars);
        tempList.add(locVar);
        final Map<VarDecl<?>, VarDecl<?>> varMap = new HashMap<>();
        for (var varDecl : tempList) {
            var newVar = Decls.Var(varDecl.getName()+"__saved", varDecl.getType());
            varMap.put(varDecl, newVar);
        }

        Expr<BoolType> x = True();
        for (var varDecl : varMap.keySet()) {
            var exp = Eq(varDecl.getRef(),varMap.get(varDecl).getRef());
            x = And(x,exp);
        }
        final Expr<BoolType> initExpr = And(Eq(locVar.getRef(),IntExprs.Int(0)),Not(saved.getRef()));
        final Stmt saveStmt = createStmts(varMap);
        final Expr<BoolType> prop = extendProp(Eq(locVar.getRef(), Int(map.get(cfa.getErrorLoc().get()))));

        return new CfaL2S<>(baseLts, vars, varMap, saveStmt, prop, locVar, map, initExpr, cfa);
    }

    private CfaL2S(final LTS<CfaState<?>, CfaAction> baseLts, final Collection<VarDecl<?>> vars,
                   final Map<VarDecl<?>, VarDecl<?>> varMap, final Stmt saveStmt, final Expr<BoolType> prop,
                   final VarDecl<IntType> locVar, final Map<CFA.Loc, Integer> map, final Expr<BoolType> initExpr, final CFA cfa) {
        this.vars = vars;
        this.prop = prop;
        this.baseLts = baseLts;
        this.varMap = varMap;
        this.saveStmt = saveStmt;
        this.locVar = locVar;
        this.map = map;
        this.initExpr = initExpr;
        this.cfa = cfa;
    }
    @Override
    public Collection<CfaAction> getEnabledActionsFor(CfaState<S> state) {
        return baseLts.getEnabledActionsFor((CfaState<S>) state).stream().flatMap(
                (CfaAction baseaction) -> {
                    Preconditions.checkArgument(baseaction.getEdges().size() == 1);
                    var edge = baseaction.getEdges().get(0);
                    var assignLoc = AssignStmt.of(locVar,IntExprs.Int(map.get(edge.getTarget())));
                    var saveAndAssignLoc = SequenceStmt.of(List.of(saveStmt,assignLoc));
                    return Stream.of(
                            xtendr.extend(baseaction, assignLoc),
                            xtendr.extend(baseaction, saveAndAssignLoc)
                    );
                }
        ).toList();
    }

    private static Stmt createStmts(final Map<VarDecl<?>, VarDecl<?>> varMap){

        ArrayList<Stmt> result = new ArrayList<>(Collections.singleton(SkipStmt.getInstance()));
        var saveList = new ArrayList<Stmt>();

        for (var varDecl : varMap.keySet()) {
            saveList.add(AssignStmt.of((VarDecl<Type>) varMap.get(varDecl), (Expr<Type>) varDecl.getRef()));
        }

        saveList.add(AssignStmt.of(saved, True()));
        var saveSequence = SequenceStmt.of(saveList);
        result.add(saveSequence);

        return saveSequence;
    }

    public static Expr<BoolType> extendProp(Expr<BoolType> prop){
        Expr<BoolType> p = prop;


        for (var varDecl : varMap.keySet()) {
            var exp = Eq(varMap.get(varDecl).getRef(),varDecl.getRef());
            p = And(p,exp);
        }

        return Not(And(p,prop,saved.getRef()));
    }

    public Iterable<VarDecl<?>> getAllVars() {
        return varMap.keySet();
    }

    public Expr<BoolType> getInitExpr() {
        return initExpr;
    }
}
