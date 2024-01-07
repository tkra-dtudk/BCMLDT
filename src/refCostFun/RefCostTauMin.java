package refCostFun;

import network.OD;

/**
 * The reference cost function  <i>min*tau</i>.
 * @author mesch
 *
 */
public class RefCostTauMin extends RefCostFun{
    /**
     * Public-access main parameter.
     */
    public double tau;
    
    /**Sole, default constructor.
     * @param tau the relative threshold
     */
    public RefCostTauMin(double tau) {
	this.tau = tau;
    }

    /**
     * Returns the reference cost <i>min*tau</i> 
     * on the od.
     * @param od the OD
     * @return the reference cost <i>min*tau</i> 
     */
    @Override
    public double calculateRefCost(OD od) {

    	return od.getMinimumCost() * tau;
    }

}
