package choiceModel;

import network.Path;

/**
 * Path size logit RUM.
 * 
 * @author mesch
 * @see RUM
 * @see RouteChoiceModel
 */
public class PSL extends RUM {
    
    public PSL() {
	
    }
    
    @Override
    public double computeEnumeratorInProbabilityExpression(Path path) {
	return Math.exp(-1*theta*(path.genCost + betaPS * Math.log(path.PS)));
    }

    @Override
    public String getTypeAsString() {
	return "PSL";
    }

}
