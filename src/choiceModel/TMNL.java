package choiceModel;

import network.Path;
import refCostFun.RefCostFun;

/**
 * The TMNL differs from most other RUMs by the fact that it 
 * its definition involves and upper reference cost. Note that in
 * almost all cases, the TMNL would be used in the context of an RSUET,
 * where the reference cost {@code omega} equals the threshold reference
 * cost of the RSUET. This must be explicitly defined, however. 
 * @author mesch
 *
 */
public class TMNL extends RUM {
    /**
     * The MNL does not include a path size term, and the inclusion of one should be
     * implemented in a different class (TPSL). To enforce this, the path size factor
     * in the TMNL is set to a {@code final} value of 0. 
     */
    public static final double betaPS = 0; //
    
    /**
     * The upper reference cost function that defines the point of truncation.
     */
    public RefCostFun omega;
    
    /**
     * Only constructor; requires specification of the reference cost function. 
     * @param refCostFun the threshold function {@code omega}
     */
    public TMNL(RefCostFun refCostFun) {
	omega = refCostFun;
    }
    
    /**
     * Does not use the path size factor.
     * @see RUM#computeEnumeratorInProbabilityExpression(Path)
     */
    @Override
    public double computeEnumeratorInProbabilityExpression(Path path) {
	return Math.max(Math.exp(-1*theta*(path.genCost - omega.calculateRefCost(path.getOD())))-1,0);
    }

    @Override
    public String getTypeAsString() {
	return "TMNL";
    }
}
