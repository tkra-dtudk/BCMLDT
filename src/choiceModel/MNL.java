package choiceModel;

import network.Path;

/**
 * The most generic random utility model.
 * @author mesch
 */
public class MNL extends RUM {
    /**
     * The MNL does not use a path size term in its utility 
     * specification, and therefore its path size factor's
     * only appropriate value is 0.
     */
    public static final double betaPS = 0;

    public MNL() {
    }
    
    /**
     * Does not use the path size factor.
     * @see RUM#computeEnumeratorInProbabilityExpression(Path)
     */
    @Override
    public double computeEnumeratorInProbabilityExpression(Path path) {
	return Math.exp(-1*theta*(path.genCost));
    }

    @Override
    public String getTypeAsString() {
	return "MNL";
    }
    
    
    
}
