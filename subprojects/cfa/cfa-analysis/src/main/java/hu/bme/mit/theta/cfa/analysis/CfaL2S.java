package hu.bme.mit.theta.cfa.analysis;

import com.google.common.base.Preconditions;
import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
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

public class CfaL2S<S extends ExprState> implements LTS<CfaState<S>,CfaAction> {

    private final Expr<BoolType> prop;
    private final Collection<VarDecl<?>> vars;
    private final VarDecl<IntType> locVar;
    private final Map<CFA.Loc, Integer> map;
    private final Expr<BoolType> initExpr;
    private CfaActionExtender xtendr;
    public Map<VarDecl<?>, VarDecl<?>> varMap;
    public Stmt saveStmt;
    CFA cfa;
    LTS<? super CfaState<S>, CfaAction> baseLts;
    VarDecl<BoolType> saved = Decls.Var("saved",BoolType.getInstance());

    public CfaL2S(LTS<? super CfaState<S>, CfaAction> baseLts,
                   CFA cfa,
                   Collection<VarDecl<?>> vars) {
        this.locVar = Decls.Var("loc", Int());
        this.cfa = cfa;

        this.vars = vars;


        int i = 0;
        this.map = new HashMap<>();
        for (var x : cfa.getLocs()) {
            map.put(x, i++);
        }

        var tempList = new ArrayList<>(vars);
        tempList.add(locVar);
        this.varMap = new HashMap<>();
        for (var varDecl : tempList) {
            var newVar = Decls.Var(varDecl.getName()+"__saved", varDecl.getType());
            varMap.put(varDecl, newVar);
        }

        Expr<BoolType> x = True();
        for (var varDecl : varMap.keySet()) {
            var exp = Eq(varDecl.getRef(),varMap.get(varDecl).getRef());
            x = And(x,exp);
        }
        Expr<BoolType> initExpr = Eq(locVar.getRef(),IntExprs.Int(0));
        this.initExpr = And(initExpr,Not(saved.getRef()));
        this.xtendr = new CfaActionExtender();
        this.baseLts = baseLts;
        this.saveStmt = getStmts();
        this.prop = extendProp();


    }

    public static <P extends Prec,S extends ExprState> CfaL2S<S> create(LTS<? super CfaState<S>, CfaAction> baseLts,
                                                                        CFA cfa,
                                                                        Collection<VarDecl<?>> vars)
    {
        return new CfaL2S<>(baseLts, cfa, vars);
    }

    @Override
    public Collection<CfaAction> getEnabledActionsFor(CfaState<S> state) {
        return baseLts.getEnabledActionsFor(state).stream().flatMap(
                (CfaAction baseaction) -> {
                    Preconditions.checkArgument(baseaction.getEdges().size() == 1);
                    var edge = baseaction.getEdges().get(0);
                    var assignLoc = AssignStmt.of(locVar,IntExprs.Int(map.get(edge.getTarget())));
                    var saveAndAssignLoc = SequenceStmt.of(List.of(saveStmt,assignLoc));
                    // Seq(NonDet(skip, save), assignLoc, stmt)
                    // =NonDet(Seq(skip,assignLoc, stmt), Seq(save, assignLoc, stmt))
                    // =[Seq(skip,assignLoc, stmt), Seq(save, assignLoc, stmt)]
                    // =Seq(assignLoc, stmt), Seq(save, assignLoc, stmt)
                    // =Seq(assignLoc, stmt), Seq(Seq(save, assignLoc), stmt)
                    return Stream.of(
                            xtendr.extend(baseaction, assignLoc), 
                            xtendr.extend(baseaction, saveAndAssignLoc)
                    );
                }
        ).toList();
    }

    public Stmt getStmts(){

        ArrayList<Stmt> result = new ArrayList<>(Collections.singleton(SkipStmt.getInstance()));
        var saveList = new ArrayList<Stmt>();

        for (var varDecl : varMap.keySet()) {
            saveList.add(AssignStmt.of((VarDecl<Type>) varMap.get(varDecl), (Expr<Type>) varDecl.getRef()));
        }

        saveList.add(AssignStmt.of(saved, True()));
        var saveSequence = SequenceStmt.of(saveList);
        result.add(saveSequence);

        //return NonDetStmt.of(result);
        return saveSequence;
    }

    public Expr<BoolType> extendProp(){
        Expr<BoolType> p = True();
        var prop = Eq(locVar.getRef(), Int(map.get(cfa.getErrorLoc().get())));

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

    public Expr<BoolType> getInitExpr() {
        return initExpr;
    }
}
