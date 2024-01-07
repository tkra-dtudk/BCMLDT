package refCostFun;

import network.OD;

/**
 * Reference cost function <i>min + delta</i>. 
 * @author mesch
 *
 */
public class RefCostMinPlusDelta extends RefCostFun {
    public double delta;
    
    /**
     * @param delta the absolute cost limit from the minimum cost
     */
    public RefCostMinPlusDelta (double delta) {
	this.delta = delta;
    }
    
    @Override
    public double calculateRefCost(OD od) {
	return od.getMinimumCost()  + delta;
    }

}
