package refCostFun;

import network.OD;

/**
 * Infinite reference cost class, mostly for use in the 
 * RSUE implementation.
 * 
 * @author mesch
 *
 */
public class RefCostInf extends RefCostFun {

    public RefCostInf() {

    }

    /**
     * @return always returns {@code Double.POSITIVE_INFINITY}
     */
    @Override
    public double calculateRefCost(OD od) {
	return Double.POSITIVE_INFINITY;
    }

}
