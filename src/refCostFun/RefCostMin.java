package refCostFun;

import network.OD;

/**
 * Returns the minimum cost route cost
 *@author mesch
 */

public class RefCostMin extends RefCostFun{

    @Override
    public double calculateRefCost(OD od) {
	return od.getMinimumCost();
    }
    
}
