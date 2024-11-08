package hu.bme.mit.theta.xsts.analysis;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;

import java.util.Collection;

// Honnan jön a formalizmusfüggő infó? leszármazott vs lambdák
// Modell a konstruktorban vs modell függvényparaméter

public abstract class GenericL2S<S extends State, A extends Action, ModelType> {

    public GenericL2S(

    ) {

    }

    Expr<BoolType> getExtendedProp();

    Collection<VarDecl<?>> getStructuralVars();
}
