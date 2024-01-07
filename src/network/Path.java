package network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import choiceModel.RSUET;
import choiceModel.RouteChoiceModel;

public class Path implements Comparable<Path>{	

	/**
	 * Boolean variable showing if a path has ever been used.
	 */

	private boolean hasBeenUsed;

	/**
	 * Path as an ordered set of edges, e.g. (1,5) (5,8) (8,1)
	 * @see Edge
	 */
	ArrayList<Edge> edges; 

	/**
	 * the flow on this path
	 */
	private double flow = 0; 

	/**
	 * Auxiliary flow used in equilibrium calculations
	 */
	private double auxFlow = 0; 

	/**
	 * Total length of this path. This usually
	 * does not change, as opposed to the 
	 * travel time, which varies with network
	 * conditions.
	 */
	double length;

	/**
	 * Generalised cost on path ("actual" cost), but without path size correction term.
	 * Generally, this should be the linear, additive part of the deterministic 
	 * utility, so that it can be defined as the sum of generalized cost on the
	 * edges of the path.
	 */
	public double genCost;

	/**
	 * a field which is used for calculations in the restricted
	 * inner mast problem.
	 * @see Network#restrictedInnerMasterProblem(choiceModel.RUM, double)
	 * @see Network#unrestrictedMasterProblemInnerLogit(choiceModel.RUM, refCostFun.RefCostFun, double)
	 */
	public double enumeratorInProbabilityExpression;
	public double enumeratorInDetourExpression;

	//TODO to be replaced with local variables in appropriate methods eventually 
	/**
	 * Probability of choosing this path conditional on some choice set.
	 */
	public double p; 

	/**
	 * Transformed cost used to calculate the relative used gap, to
	 * allow a DUE-like algorithm in the RSUET.
	 * 
	 * @see Network#relGapUsed()
	 * @see Path#updateTransformedCost(RouteChoiceModel)
	 */
	double transformedCost = 0; 

	/**
	 * The OD that this path belongs to
	 */
	private OD od; 

	/**
	 * Path Size factor for use in PSL
	 */
	public double PS = 1; 

	/**
	 * Used by algorithms to remove and redistribute flow.
	 * @see RSUET#redistributeFlowOnMarkedRoutesAccordingToProbability(Network, int)
	 */
	public boolean markedForRemoval;

	private double maxRelativeSubtourLength;

	public int[] nodeSeq;

	public Network network;

	/**
	 * Constructs a path from a set of edges, with
	 * an explicitly specified OD relation to facilitate
	 * certain method calls.
	 * 
	 * @param edges a list of edges that make up the path
	 * @param od the OD relation that the path belongs to
	 */
	public Path(ArrayList<Edge> edges, OD od) {
		this(edges,od,false, 0.);
	}

	public Path(ArrayList<Edge> edges, OD od, double relativeSubtourLength) {
		this(edges,od,false, relativeSubtourLength);
	}

	public Path(ArrayList<Edge> edges, OD od, boolean hasBeenUsed, double relativeSubtourLength) {
		//		this.nodeSeq = nodeSeq;
		this.edges = edges;
		this.maxRelativeSubtourLength = relativeSubtourLength;
		length = 0;
		for (int i = 0; i < edges.size(); i++) {
			length += edges.get(i).getLength();
		}
		this.od = od;
		this.hasBeenUsed = hasBeenUsed;
	}

	/**
	 * The natural ordering of paths uses their
	 * generalized cost. This make it possible to
	 * sort a list of paths using 
	 * {@link Collections#sort(java.util.List)}.
	 */
	@Override
	public int compareTo(Path o) {//Allows sorting of paths by cost (ratio)
		return Double.compare(this.genCost,o.genCost);
	}

	/**
	 * Computes the enumerator in the probability expression 
	 * of the route choice model (most often simply a RUM)
	 * using the route choice model's own method of the same 
	 * name.
	 * 
	 * @param rcm the route choice model that will determine the value to return
	 * @return a number which corresponds for e.g. the MNL to exp(-theta*V).
	 * @see RouteChoiceModel#computeEnumeratorInProbabilityExpression(Path)
	 */
	public double computeEnumeratorInProbabilityExpression(RouteChoiceModel rcm){
		return rcm.computeEnumeratorInProbabilityExpression(this);
	}

	/**
	 * Calculate the transformed cost of this path. In general, it is 
	 * equal to the flow on the route divided by the value returned by
	 * {@linkplain Path#computeEnumeratorInProbabilityExpression(RouteChoiceModel)},
	 * but if the flow on the route is 0, 0 is return before evaluating the latter
	 * to save some computation time.
	 * 
	 * @param rcm the route choice model to determine the transformed cost to return
	 * @return the transformed cost of the route according to the 
	 * route choice model
	 */
	public double computeTransformedCost(RouteChoiceModel rcm){
		if (this.flow == 0) return 0; 
		return this.flow/computeEnumeratorInProbabilityExpression(rcm);
	}

	/**
	 * Comparison of to paths by their node sequence.
	 * 
	 * @param anotherPath the path to compare to
	 * @return true if this path has the same node
	 * sequence as {@code anotherPath}, false otherwise
	 */
	public boolean equals(Path anotherPath) {
		int thisSize = this.edges.size();
		if (thisSize != anotherPath.edges.size()) return false;
		else {
			for (int i = 0; i < thisSize; i++) {
				if (this.edges.get(i).getTail() != anotherPath.edges.get(i).getTail()) return false;
			}
			if (this.edges.get(thisSize-1).getHead() != anotherPath.edges.get(thisSize-1).getHead()) return false;
		}
		return true;
	}

	public double getAuxFlow() {
		return auxFlow;
	}

	public int getD(){
		return this.od.D;
	}

	/**
	 * @return the flow
	 */
	public double getFlow() {
		return flow;
	}

	public boolean getHasBeenUsed() {
		return hasBeenUsed;
	}

	public int getO(){
		return this.od.O;
	}

	public OD getOD() {
		return this.od;
	}

	public double getMaximumRelativeSubtourLength() {
		return this.maxRelativeSubtourLength;
	}

	/**
	 * This simply adds the flow on the path to the network,
	 * on top of what is already there. 
	 */
	public void load(){ 
		for (Edge edge: edges) {
			edge.addFlow(this.flow);
		}
	}

	public void setAuxFlow(double auxFlow) {
		this.auxFlow = auxFlow;
	}

	/**
	 * @param flow the flow to set
	 */
	public void setFlow(double flow) {
		if (Double.isNaN(flow)) {
			throw new Error("Flow may only be set to a real number.");
		}
		this.flow = flow;
	}

	public void setHasBeenUsed(boolean hasBeenUsed) {
		this.hasBeenUsed = hasBeenUsed;
	}

	/**
	 * Printing a path shows the node sequence, cost and flow.
	 * 
	 * @return String the string to be shown when printing
	 * an object of type Path
	 */
	@Override
	public String toString() {
		String string = "";
		int iterateTo = edges.size();
		for (int i = 0; i < iterateTo ; i++) {
			string = string+edges.get(i).getTail() + "->";
		}
		string = string + edges.get(iterateTo-1).getHead();

		return "Path: "+string+". genCost: "+genCost + ". Flow: " +getFlow();
	}

	/**
	 * Sets the cost of the path {@code genCost}
	 * to the sum of costs of its edges.
	 * 
	 * @return the cost which was set
	 */

	public double updateMaximumRelativeSubtourLength() {
		maxRelativeSubtourLength = 0;
		for (int i = 0; i < nodeSeq.length -1; i++) {
			int startNodeInt = nodeSeq[i];
			double genCost = 0;	
			for(int j = i +1; j <nodeSeq.length; j++ ) {
				int endNodeInt = nodeSeq[j];
				int intermediateNodeInt = nodeSeq[j-1];
				genCost += network.edgesNodePair.get(intermediateNodeInt).get(endNodeInt).getGenCost();
				double shortestSubpath = network.dijkstraDists.get(startNodeInt).get(endNodeInt);
				maxRelativeSubtourLength = Math.max(genCost / shortestSubpath, maxRelativeSubtourLength);
				//System.out.print("maxRelativeSubtourLength: " + maxRelativeSubtourLength + " startNode: " + startNodeInt + " endNode: "+endNodeInt+ " gencost: "+ genCost+" shortest path Cost: " + shortestSubpath + "\n");
				
				if(maxRelativeSubtourLength > network.getLocalMaximumCostRatio()) {
					return maxRelativeSubtourLength;
				}
			}
		}
		return maxRelativeSubtourLength;
	}

	public double updateCost(double detourednessParameter){
		double genCost = 0;
		for (Edge edge: edges) {
			genCost += edge.getGenCost(); // when uselocalstorage, sometimes  edges have gencost = null
		}
		//if(detourednessParameter > 0) {
//			genCost += detourednessParameter * getMaximumRelativeSubtourLength();
	//	}
		this.genCost = genCost;
		return this.genCost;
	}

	/**
	 * Calculates and updates the 
	 * transformed cost of this path
	 * 
	 * @param rcm the route choice model to use
	 * to calculate the transformed cost
	 * @return the cost which was sets
	 */
	public double updateTransformedCost(RouteChoiceModel rcm){
		double threshold = rcm.calculateThreshold(this.od);
		if (this.genCost < threshold) {
			this.transformedCost = computeTransformedCost(rcm);
		} else {
			this.transformedCost = 0;
		}
		return transformedCost;

	}
}
