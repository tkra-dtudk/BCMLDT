package refCostFun;

import network.OD;

/**
 * Abstract class of reference cost functions to 
 * better structure the different variants
 * of RSUET(Phi, Omega) models; Phi and Omega 
 * would be implemented as reference cost functions.
 * @author mesch
 *
 */
public abstract class RefCostFun {
    
    /**
     * @param od to calculate reference cost on
     * @return the reference cost
     */
    public abstract double calculateRefCost(OD od);
}
