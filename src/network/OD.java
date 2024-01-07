package network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import choiceModel.RUM;

public class OD{
	/**
	 * The origin node, represented as an 
	 * integer corresponding to the ID of the node.
	 */
	public int O;
	
	/**
	 * The ID of the destination node.
	 */
	public int D;
	
	/**
	 * The demand, for instance number of trips,
	 * that was specified in the [network]_trips.tntp
	 * file included in the folder that was referenced
	 * for construction of the network to which this
	 * {@code OD} belongs.
	 */
	public double demand;
	
	/**
	 * Set of used paths from O to D for use in RSUET
	 */
	public ArrayList<Path> restrictedChoiceSet;
	
	/**
	 * Universal choice set (or master choice set), generated 
	 * by calling {@link Network#generateInitialRestrictedChoiceSets()}.
	 */
	ArrayList<Path> R;
	
	public ArrayList<Path> pseudoR;
	
	/**
	 * Used by private methods in the {@linkplain Network} class.
	 */
	boolean pathWasAddedDuringColumnGeneration = false;

	/**
	 * Used by private methods in the {@linkplain Network} class.
	 */
	public boolean pathWasRemovedDuringLastIteration = false;

	/**
	 * Field containing the minimum generalized cost
	 * among routes in {@code paths}. Is updated by
	 * {@link Network#updatePathCosts()}
	 */
	private double minimumCost;
	/**
	 * Field containing the minimum transformed cost
	 * among routes in {@code paths}. Is updated by
	 * {@link Network#updateTransformedCosts(choiceModel.RouteChoiceModel)}
	 */
	private double minimumTransformedCost;

	/**
	 * Minimal constructor. 
	 * 
	 * @param O the origin node ID
	 * @param D the destination node ID 
	 * @param demand the demand from O to D
	 */
	public OD(int O, int D, double demand){
		this.O = O;
		this.D = D;
		this.demand = demand;
		restrictedChoiceSet = new ArrayList<Path>();
		R = new ArrayList<Path>();
	}

	/**
	 * Adds a path to the {@code paths} list.
	 * 
	 * @param path the path to be added
	 */
	public void addPath(Path path){
		this.restrictedChoiceSet.add(path);
	}

	/**
	 * Adds a path to the universal choice set.
	 * 
	 * @param path the path to be added
	 * @see Network#generateUniversalChoiceSets()
	 */
	public void addPathR(Path path){
		this.R.add(path);
	}

	public double getMinimumCost() {
		return minimumCost;
	}
	public double getMinimumTransformedCost() {
		return this.minimumTransformedCost;
	}

	public double getMinPathDist(){
		double min = restrictedChoiceSet.get(0).genCost;
		for (int i = 1; i < restrictedChoiceSet.size(); i++) {
			if (restrictedChoiceSet.get(i).genCost < min) {
				min = restrictedChoiceSet.get(i).genCost;
			}
		}
		return min;
	}

	public void setMinimumCost(double minCost) {
		this.minimumCost = minCost;

	}

	public void setMinimumTransformedCost(double transCost) {
		this.minimumTransformedCost = transCost;

	}

	public void updateMinimumCost() {
		minimumCost = Collections.min(restrictedChoiceSet).genCost;
	}

	
	public void updatePathSizeFactors(RUM rum) {
		updatePathSizeFactors(rum,restrictedChoiceSet);
	}

	/**
	 * updates the path size factors of paths 
	 * in the restricted choice set of this OD. 
	 * Generalized cost is used to determine similarity
	 * between routes, which means that the path size 
	 * factors must be updated when network LoS changes.
	 * <p>
	 * If the path path size utility function parameter
	 * {@code betaPS} of the specified RUM equals 0,
	 * the method continues without updating the path 
	 * size factors.
	 * 
	 * @param rum the RUM from which to retrieve a path size
	 * factor. All RUM's have path size factors, and those
	 * that do not use it should specify {@code betaPS = 0}
	 * to not waste computation time updating the path size
	 * factors.
	 * 
	 * @param choiceSet the choice set on which to update path
	 * size factors; this is usually an ArrayList of paths.
	 */
	public void updatePathSizeFactors(RUM rum, ArrayList<Path> choiceSet) {
		//don't bother updating path size factors if they are not going to be used
		if (rum.betaPS == 0) {
			return;
		}
		//reset deltas
		double sum;
		for (Path path: choiceSet) {
			for (Edge edge: path.edges) {
				edge.setNumPathsWithEdge(0);
			}
		}
		//			Count link occurences
		for (Path path: choiceSet) {
		    for (Edge edge: path.edges) {
				edge.setNumPathsWithEdge(edge.getNumPathsWithEdge()+1);
			}
		}
		//			calculate PS factors
		for (Path path : choiceSet) {
			sum = 0; 
			double pathCost = path.genCost;
			for (Edge edge : path.edges) {
				double delta = edge.getNumPathsWithEdge();
				sum += edge.getGenCost() / (pathCost * delta);
			}
			if (!Double.isFinite(sum)) {
				throw new IllegalArgumentException();
			}
			path.PS = sum;
		}
	}
}
