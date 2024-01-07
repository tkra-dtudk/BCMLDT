package choiceModel;

import network.Edge;
import network.Path;

/** 
 * This abstract class means to structure the different types of random utility
 * models that exist; note that a RUM is not a route choice model, but rather
 * route choice models may have associated with them some RUM (even though this 
 * does not necessarily have to be the case, for instance in the case of the 
 * DUE). Also note that all RUMs have allocated to them a path size factor betaPS;
 * in cases where this is not used, such as with the MNL, it should be set to 0
 * for clarity. 
 * @author mesch
 *
 */
public abstract class RUM {
	/**
	 * Distributional parameter of the RUM, which should be used in the
	 *  implementation of {@code computeEnumeratorInProbabilityExpression}. 
	 */
	public double theta = 0.5;
	
	public double thetaLocalD = 0.5;

	/**
	 * Path size parameter with a default value of -3, although many RUMs
	 * would not use this value; such RUM should explicitly override the
	 * inherited default value with a {@code final} value of 0.
	 */
	public double betaPS = 0;

	/**
	 * Value of time (VoT) used in {@code computeEnumeratorInProbabilityExpression}.
	 */
	public static double betaTime = 1.0;

	/**
	 * The effect of time on cost; on networks like Sioux Falls where the link length
	 * is arbitrarily set to be the same as the link free flow travel time, this should 
	 * be set to 0.
	 */
	public static double betaLength = 0.0;

	/**
	 * maybe somewhat of a misnomer, "generalized cost" in this context means the linear
	 * and additive deterministic part of route cost, using generalized cost parameters, but not 
	 * taking into account the {@code theta} value of the RUM. As such, it never includes 
	 * the path size term. This method may be overwritten, but generally it should not be, and
	 * it must always constitute a linear part of route cost. This cost summed over paths is 
	 * the cost that is used by reference cost functions in the RSUET; this can sometimes give
	 * difficulties when path size terms become included, since the similarity of routes may 
	 * make them less attractive by reducing their choice probability, but never by affecting
	 * if they were considered in the first place, since the path size term is not part of the 
	 * threshold reference cost that determines when route probability becomes 0.
	 * 
	 * @param edge the edge of
	 * @return the generalized cost which was calculated
	 */
	public double genCost(Edge edge) {
		return calculateStandardGenCost(edge);
	}

	/**
	 * Standard linear route choice specification. At the time of writing, the network
	 * package does not easily allow more dimensions of link properties to be taken into
	 * account; for instance, tolls (monetary costs) are not yet supported. If this is to be included,
	 * reading it must first be implemented in the network constructor; secondly, the edge class
	 * must have added to it a field that represents the cost; and thirdly, the linear additive
	 * cost specification must expanded in the method below.
	 * @param edge the edge to calculate to cost of
	 * @return the cost of the edge
	 */
	private double calculateStandardGenCost(Edge edge) {
		return betaTime * edge.getTime() + betaLength * edge.getLength();
	}

	/**
	 * This crucial method determines the probability of routes by calculating the enumerator in
	 * the RUM probability expression; for an MNL this would be: exp(-theta*v_i) in 
	 * p_i = exp(-theta*V_i)/sum_i(exp(-theta*V_i)). V_i in this sense corresponds to 
	 * the {@code genCost(i)}, where {@code i} is an edge.
	 * 
	 * @param path the path on which to compute the probability expression enumerator
	 * @return a numerical value with no mathematical meaning before it is related to 
	 * the sum of corresponding values for the remaining used paths in the OD relation. 
	 * Note that numerical issues may be encountered when the value of theta and/or generalized
	 * costs exceed some value because of the limitations of the size of values supported by 
	 * the Double class. 
	 */
	public abstract double computeEnumeratorInProbabilityExpression(Path path);

	/**
	 * @return a string which should match the name of the RUM class. 
	 */
	public abstract String getTypeAsString();
}
