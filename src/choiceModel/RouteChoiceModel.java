package choiceModel;

import java.io.IOException;

import auxiliary.ConvergencePattern;
import network.Network;
import network.OD;
import network.Path;

/**
 * Abstract class which has at least a RUM 
 * as part of it.
 * @author mesch
 * @see RUM
 *
 */
public abstract class RouteChoiceModel {
    protected RUM rum;
    
    /**
     * Route choice models in this context are objects 
     * which can model the assignment of demand as flow 
     * on a network. They may have a random utility model,
     * and must always have the methods 
     * 
     * @param network the {@linkplain Network} to get to equilibrium in.
     * @return a convergence pattern that shows how the solution algorithm
     * fared
     * @throws IOException 
     */
    public abstract ConvergencePattern solve(Network network) throws IOException;

    protected void setRum(RUM rum2) {
	this.rum = rum2;
    }

    //TODO reconsider if route choice models should have this type of method; 
    //probably this should be replaced by a method called from the RUM, or even in the 
    //    specific RSUET class only. 
    public abstract double calculateThreshold(OD od);
    
    public abstract double computeEnumeratorInProbabilityExpression(Path path);
    
    public RUM getRUM() {
	return rum;
    }
    
}
