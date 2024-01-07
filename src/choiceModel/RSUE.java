package choiceModel;

import network.OD;
import refCostFun.RefCostFun;
import refCostFun.RefCostInf;
/**
 * Although it is not computationally the most efficient, the RSUE is 
 * implemented as a special case of the RSUET where the threshold 
 * is infinity. This should be theoretically sound, and saves a 
 * lot of coding.
 * @author mesch
 *
 */
public class RSUE extends RSUET {
    /**
     * The "threshold" must by definition be 0 for the integrity
     * of the class to be maintained, since it is implemented as
     * and RSUET.
     */
    public static final RefCostFun omega = new RefCostInf();
    
    public RSUE(RUM rum, RefCostFun phi) {
//    	super(rum, phi, omega, 1);
    	super(rum, phi, omega,1, true, 1, true, true); 
    }

    @Override
    public double calculateThreshold(OD od) {
        return Double.POSITIVE_INFINITY;
    }
    
}
