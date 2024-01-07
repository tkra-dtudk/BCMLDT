package choiceModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

import auxiliary.ConvergencePattern;
import auxiliary.StopWatch;
import auxiliary.Utils;
import network.Network;
import network.OD;
import network.Path;
import refCostFun.RefCostFun;
import refCostFun.RefCostMin;
import refCostFun.RefCostMinPlusDelta;
import refCostFun.RefCostTauMin;

/**
 * This class of route choice models, originated from Thomas Kjær Rasmussen
 * of the Technical University of Denmark, has extremely attractive computational
 * properties when the lower reference cost {@code phi} function equals {@code RefCostMin}
 * @author mesch
 * @see RUM
 * @see RefCostFun
 */
public class RSUET extends RouteChoiceModel {
	/**field {@code rum} is inherited from superclass 
	 * {@linkplain RouteChoiceModel}.
	 */

	boolean consEnumIte;


	/**
	 * d for gamma in RSUET(min,omega)
	 */
	public int d = 2;

	double demandScale;

	/**
	 * The maximum allowed sum of gap measures for the
	 * for a solution to be considered an equilibrium.
	 */
	public double epsilon = 0.00005;

	/**
	 * The minimum number of routes present in a restricted choice set 
	 * for {@code doThresholdConditionPhase} to remove one path. By default,
	 * this is set to 2 so that at least one route is present at equilibrium
	 * and that routes may never be removed when there are no other used 
	 * paths in the same OD relation.
	 */
	public int Nmin = 2;

	/**
	 * Threshold-violating paths are potentially removed in algorithms by 
	 * {@code doThresholdConditionPhase} if and only if the iteration
	 * number is no smaller than {@code Kmin}. 
	 */
	public int Kmin = 7;

	/**
	 * The iteration number at which the algorithm will "give up" trying
	 * to reach equilibrium.
	 */
	public int itmax = 3;
	
	public boolean flowDepTravelTime = true;
	/**
	 * Reference cost function that determines which routes must be used
	 */
	RefCostFun phi;

	/**
	 * Reference cost function that determines which routes must be unused
	 */
	RefCostFun omega;

	/**
	 * This integer is used by {@linkplain Utils#gamma(int, int)} to determine
	 * how much the auxiliary solution is trusted during the algorithmic phase.
	 * The higher the integer, the more trusted is the provisional flow. The value
	 * specified here is used for {@code cutUniversalChoiceSetAlgorithm} when the
	 * RUM is of type TMNL.
	 * @see Utils#gamma(int, int)
	 * @see RSUET#cutUniversalChoiceSetAlgorithm(Network)
	 */
	public int dForUniversalChoiceSetAlgTMNL = 2;

	/**
	 * This integer is used by {@linkplain Utils#gamma(int, int)} to determine
	 * how much the auxiliary solution is trusted during the algorithmic phase.
	 * The higher the integer, the more trusted is the provisional flow. The value
	 * specified here is used for {@code cutUniversalChoiceSetAlgorithm} when the
	 * RUM is of type MNL or PSL.
	 * @see Utils#gamma(int, int)
	 * @see RSUET#cutUniversalChoiceSetAlgorithm(Network)
	 */
	public int dForUniversalChoiceSetAlgMNL = 2;

	/**
	 * To avoid manipulating irrelevant routes in the universal choice set in 
	 * {@code cutUniversalChoiceSetAlgorithm}, only routes where 
	 * {@code path.gencost <= maximumCostRatio*path.od.getMinimumCost()} are
	 * considered.
	 */
	public double maximumCostRatio = 100;
	public double localMaximumCostRatio;

	private double bound;


	private boolean useOnlyConsideredPaths;
	private boolean exportConsideredPaths;


	public static int doInitialRSUET;
	public static int laterIteration;

	/**
	 * Only class constructor, since the RSUET requires two reference costs
	 * in order to make sense. 
	 * @param rum the RUM to be used 
	 * @param phi the lower reference cost function
	 * @param omega the upper reference cost function
	 * @see RefCostFun
	 */
	public RSUET(RUM rum, RefCostFun phi, RefCostFun omega, double demandScale, boolean consEnumIte, double bound,
			              boolean useOnlyConsideredPaths, boolean exportConsideredPaths) {
		this.setRum(rum);
		this.phi = phi;
		this.omega = omega;
		this.demandScale=demandScale;
		this.consEnumIte=consEnumIte;
		this.bound=bound;
		this.useOnlyConsideredPaths = useOnlyConsideredPaths;
		this.exportConsideredPaths = exportConsideredPaths;
	}


	@Override
	public double calculateThreshold(OD od) {
		return omega.calculateRefCost(od);
	}

	
	
	// Starting initial RSUET to generate initial flow solution!
	
	
	private ConvergencePattern columnGenerationAlgorithm(Network network, RefCostFun phi, RefCostFun omega, String... varargs) {
		boolean suppress = false; //suppress output to console (time and gap measures)
		for (String option : varargs) {
			String optionLowerCase = option.toLowerCase();
			if (optionLowerCase == "suppress" || optionLowerCase == "supress") {
				suppress = true;
			} else {
				System.err.println("Option " + option + " was not recognized and will be ignored. Valid options are:");
				System.err.println("printGaps");
			}
		}
		StopWatch timer = new StopWatch();
		timer.start();
		// Step 0: Initialization
		int n = 1; // Iteration counter
		int nmax = itmax;
		double gap; // relative gap measure
		double gapUnused = 1;
		double gapUsed = 1; // Initially, this is 1
		double gapLocalDetour=1;

		ConvergencePattern conv = new ConvergencePattern();
		if (!suppress) conv.printGapHeader();

		network.resetNetwork(); // Set network flows to 0, restricted choice sets to
		// empty
		network.updateEdgeCosts(rum);
		network.allOrNothing(); // Perform all-or-nothing assignment
		network.loadNetwork(); // Load all-or-nothing assignment results
		if (flowDepTravelTime=true) network.updateEdgeCosts(rum); // Update edge costs
		//	network.updateEdgeCosts(rum); // Update edge costs
		network.updatePathCosts(); // Update path costs
		network.updateTransformedCosts(this);
		n++;

		boolean doThresholdCondition = true;
		if (this instanceof RSUE) doThresholdCondition = false; //to save a bit of computation time

		while (n <= nmax) { // Outer loop
			double gamma = Utils.gamma(n, d);// Calculate gamma
			// Step 1: Column generation
			boolean success = network.columnGeneration();
			if (!success) {
				System.err.println("Warning! RSUET did not converge.");
				return conv;
			}

			//Since a path was added in the column generation, the path size factors 
			//need updating (cost of path has been updated) BUT only after relative gap has been evaluated

			// **Step 4: Convergence evaluation. This is moved here to avoid
			// performing an extra shortest path search (dijkstra).
			double gapUnusedBelow = relGapBelow(network);
			double gapUnusedAbove = relGapAbove(network);
			gapUnused = gapUnusedBelow + gapUnusedAbove;
			gap = gapLocalDetour + gapUnused;
			conv.addIteration(n, gapUnusedBelow, gapUnusedAbove, gapLocalDetour);

			if (!suppress)
				conv.printCurrentIteration();
			if (gap < epsilon && n > 2) {
				n--;
				System.out.println("Initial RSUET terminated with relative gap less than epsilon. Iteration: " + n);
				break;
			}

			//network.updatePathSizeFactorsWherePathsWereAdded(rum);

			// Step 2: Restricted master problem phase
			network.restrictedInnerMasterProblem(rum, gamma); // Uses inner logit
			// instead of path swap

			// Step 3: network loading
			network.loadNetwork();
			if (flowDepTravelTime=true) network.updateEdgeCosts(rum); // Update edge costs
			//network.updateEdgeCosts(rum);	
			network.updatePathCosts();
			network.updatePathSizeFactors(rum);
			network.updateTransformedCosts(this);

			// Step 4: Threshold condition phase
			if (doThresholdCondition) doThresholdConditionPhase(network, n);

			// Step 5: Convergence evaluation phase
			// Since a shortest path search based on link costs is needed
			// to compute the gap measure, the evaluation of 
			// relGapUnused is actually performed in the next iteration
			gapUsed = network.relGapUsed();
			gapLocalDetour = network.gapLocalDetour();
			n++;


		}
		if (n >= nmax) {
			System.err.println("Warning! Initial RSUET did not converge.");
			return conv;
		}
		if (!suppress) printRSUETtime(timer.stop());
		return conv;
	}

	/**
	 * Used by the solution algorithm when the lower reference cost is {@code RefCostMin}.
	 * This is because a shortest path search can be used to determine if the used cost
	 * condition is met; "column generation" refers to the inclusion of new paths by 
	 * a shortest path search.
	 * @param network the network to solve
	 * @param varargs options to pass; "suppress" will stop the method from printing gap
	 * measures.
	 * @return the convergence pattern that the algorithm returned
	 */
// Finished initial RSUET to generate initial flow solution!
	
	
	
	
	
	
	
	// START ACTUAL ASSIGNMENT AFTER INITIALISATION
	
	
	
	
	
	
	private ConvergencePattern columnGenerationAlgorithm(Network network, String...varargs) {
		return columnGenerationAlgorithm(network, this.phi, this.omega, varargs);
	}


	@Override
	public double computeEnumeratorInProbabilityExpression(Path path) {
		return rum.computeEnumeratorInProbabilityExpression(path);
	}

	/**
	 * An inefficient, proof-of-concept experimental implementation of the RSUET
	 * when the reference cost Phi is not the  "min" function.
	 * It is not advised to use this algorithm on larger networks, since it 
	 * enumerates the universal choice set. Perhaps future implementations
	 * should instead use a k-shortest path algorithm. 
	 * 
	 * <p> To speed up computation times, this algorithm heuristically cuts
	 * off more expensive routes that with a high degree of likelihood would
	 * not have flow assigned to them anyway. 
	 * 
	 * <p> The initial flow solution used is obtained by an RSUET with similar 
	 * parameters to the TMNL.
	 *  @param network the network to solve
	 *  @return convergence pattern, with two unused gap measures: above and below
	 * @throws IOException 
	 */
	private ConvergencePattern cutUniversalChoiceSetAlgorithm(Network network) throws IOException {
		boolean doRSUET = true;																			//_______________________
		//TODO workaround: Avoid doing all-or-nothing if transformed costs would be enormous
		//if (rum.theta >= 0.75 && rum instanceof TMNL) doRSUET = false;
//		if (rum.theta >= 1.1 && rum instanceof TMNL) doRSUET = false;
//		if (doInitialRSUET == 0  && laterIteration==1) doRSUET = false;

		if (doRSUET) columnGenerationAlgorithm(network);

		ConvergencePattern conv = new ConvergencePattern();
		conv.printGapHeader();
		if (flowDepTravelTime=true) network.updateEdgeCosts(rum); // Update edge costs
		//network.updateEdgeCosts(rum);

		int mnl = getDToUseInUniversalChoiceSetAlg();

		//if the universal choice set is not generated, do it now.
		if(!useOnlyConsideredPaths) {
			network.generateUniversalChoiceSets();
		} else {
			network.loadConsideredPaths();
		}
		network.universalChoiceSetsStored = true;



		//Heuristically "cut" universal choice sets down to a more manageable size
		network.cutUniversalChoiceSets(maximumCostRatio);    //madsp: Should not be used, when constrained enumeration is used for creating the "universal" choice set.

		network.updateMaximumRelativeSubtourLength(); //This is possibly not necessary.
		network.updatePathCosts();

		//proceed to the MSA and loop until convergence is reached
		boolean isConverged = false;
		boolean hasFailed = false;
		boolean doRedistribution = false;
		int iterationNumber = 1;
		int iterationForGamma = 1;
		int maxIterations = itmax;
		int numIterationsWithOuterConvergenceBeforeRedistribution = 3;
		int iterationToReset = (doRSUET) ? 75: 150;



		if (!(rum instanceof TMNL)) iterationToReset += 25;
		int latestTimeToStartRedistribution = 100;
		int numIterationsToRampUpMnl = 10000; //disabled

		int numTimesInARowWithOuterConvergence = 0;
		while (!isConverged && !hasFailed) {
			if((iterationNumber > iterationToReset)) iterationToReset +=25;
			if (iterationNumber == iterationToReset) iterationForGamma = 30;
			if (iterationNumber == iterationToReset) System.out.println("IterationToReset: " + (iterationToReset));
			double gamma = Utils.gamma(iterationForGamma,mnl);
			network.unrestrictedMasterProblemInnerLogit(rum, omega,gamma);			 

			if (doRedistribution) {									//Check if this need to go out
				//redistributeFlowOnMostViolatingRoute();
				System.out.println("Redistributing flow");			//Check if this need to go out
				redistributeFlowOnMarkedRoutesAccordingToProbability(network,1000);	//Check if this need to go out
			}																		//Check if this need to go out

			network.loadNetwork();
			if (flowDepTravelTime=true) network.updateEdgeCosts(rum); // Update edge costs
			//network.updateEdgeCosts(rum);
			network.generateAllShortestPathTrees();
			System.out.println(
					"updating updateMaximumRelativeSubtourLength");
			network.updateMaximumRelativeSubtourLength();
			network.updatePathCosts();
			if (consEnumIte) {
				network.consEnum(bound);
			}
			if(iterationNumber == 1 && !doRSUET) {
				iterationNumber++;
				continue;
			}

			if (iterationNumber % numIterationsToRampUpMnl == 0) mnl++;

			network.updateTransformedCosts(this);
			double gapUnusedAbove = relGapAbove(network);
			double gapUnusedBelow = relGapBelow(network);
			double gapUnused = gapUnusedAbove + gapUnusedBelow;// relGapUnused(localParam);//relGapUnused(localParam);
			double gapUsed = network.relGapUsed();
			double gapLocalDetour = network.gapLocalDetour();
			System.out.println(
					"gapLocalDetour:" + gapLocalDetour);
			conv.addIteration(iterationNumber, -1, -1, gapLocalDetour);
			conv.printCurrentIteration();

			if (gapUnused <= 0.001) {
				numTimesInARowWithOuterConvergence ++;
			}

			if (numTimesInARowWithOuterConvergence >= numIterationsWithOuterConvergenceBeforeRedistribution || iterationNumber >= latestTimeToStartRedistribution) {
				doRedistribution = true;
				numTimesInARowWithOuterConvergence = 0;
			}

			if (gapUnused >= 0.3) doRedistribution = false;

			//if (gapUnused <= 0.0001 && gapUsed <= 0.01) isConverged = true;
			//if (gapUnused <= 0.0001 && gapLocalDetour <= 0.00001 && iterationNumber > 1) isConverged = true;
			if (gapLocalDetour <= 0.00001 && iterationNumber > 10) isConverged = true;

			iterationNumber++;
			iterationForGamma++;
			if (iterationNumber > maxIterations) hasFailed = true;
			
	
		}
		if (!hasFailed) {
			System.out.println("BCMLD successfully converged. Number of iterations: " + (iterationNumber-1));
			if(exportConsideredPaths) {
				network.exportConsideredPaths();
			}
		} else {
			System.out.println("BCMLD failed to converge!");
			
		}
		conv.didConverge = isConverged;
		return conv;
	}

	/**
	 * Removes routes from restricted choice sets that violate the threshold
	 * defined by the reference cost {@code omega}. Redistributes the flow
	 * on removed routes.
	 * 
	 * @param network the network which is being solved
	 * @param n the iteration number
	 * @return true is something was removed, false otherwise
	 */
	private boolean doThresholdConditionPhase(Network network, int n) {
		// Step 4: Threshold condition phase
		boolean somethingWasFlagged = false;

		// Step 4.1: flagging
		HashMap<OD,Path> flags = new HashMap<OD,Path>();// Most violating path index for each
		// od, 0 if none above threshold


		if (n >= Kmin) {
			for (HashMap<Integer, OD> m: network.ods.values()) {
				for (OD od: m.values()) {
					int numPaths = od.restrictedChoiceSet.size();
					if (numPaths >= Nmin) {
						double maxCost = -1; // Initialise MRUE (maximum ratio of
						// cost/minimum cost)
						double threshold = calculateThreshold(od);
						Path flaggedPath = null;
						for (Path path: od.restrictedChoiceSet) {// Find highest cost route
							double cost = path.genCost;
							if (cost > maxCost) { // Current greatest violation
								maxCost = cost;
								flaggedPath = path;
							}
						}
						if (maxCost > threshold) {// If greatest violation is above
							// threshold, flag this path for
							// removal
							flags.put(od,flaggedPath);
							somethingWasFlagged = true;
						}
					}
				}
			}
		}

		// Step 4.2: Removal and Redistribution
		double extraFlow; // Flow from removed route which is to be
		// redistributed
		for (OD od: flags.keySet()) {
			Path pathToRemove = flags.get(od);
			extraFlow = pathToRemove.getFlow();
			pathToRemove.setFlow(0);
			od.restrictedChoiceSet.remove(pathToRemove); // Remove flagged path from
			// od
			for (Path path : od.restrictedChoiceSet) {
				// Redistribute flow
				//path.setFlow(path.getFlow() + extraFlow * path.getFlow() / (od.demand - extraFlow));
				path.setFlow(path.getFlow() + extraFlow * path.p);

			}

		}

		// Step 4.3: network loading
		if (somethingWasFlagged) {
			network.loadNetwork();
			if (flowDepTravelTime=true) network.updateEdgeCosts(rum); // Update edge costs
			//network.updateEdgeCosts(rum);
			network.updatePathCosts();
			network.updatePathSizeFactors(rum);
			network.updateTransformedCosts(this);
		}
		return somethingWasFlagged;
	}

	/**
	 * Heuristically decides which weighting parameter for {@linkplain Utils#gamma(int, int)}
	 * to use for {@linkplain RSUET#cutUniversalChoiceSetAlgorithm(Network)}.
	 * @return the d to be used for {@linkplain Utils#gamma(int, int)} in the MSA
	 */
	private int getDToUseInUniversalChoiceSetAlg() {
		return (this.rum instanceof TMNL)? dForUniversalChoiceSetAlgTMNL: dForUniversalChoiceSetAlgMNL;
	}





	/**
	 * This method prints details about the route choice model, i.e. the RSUET.
	 * This method does not override anything, since there is no general implementation
	 * of a "print" function of {@code RouteChoiceModels}.
	 * 
	 * @param file the file to print parameters to.
	 * @throws FileNotFoundException Throws an exception if 
	 * the file could not be written.
	 */
	public void printParamToFile(File file) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(file);
		//		out.println(descriptiveHeader);
		String delimiter = ";";
		String doubleFormat = "%6.5f";

		String rumType = rum.getTypeAsString();
		out.print("RUM type");
		out.print(delimiter);
		out.println(rumType);

		if (omega instanceof RefCostMinPlusDelta) {
			RefCostMinPlusDelta omega2 = (RefCostMinPlusDelta) omega;
			out.print("Delta");
			out.print(delimiter);
			out.println(omega2.delta);
		} else if (omega instanceof RefCostTauMin){
			RefCostTauMin omega2 = (RefCostTauMin) omega;
			out.print("BCM-Bound");
			out.print(delimiter);
			out.println(omega2.tau);
		}

		out.print("Theta");
		out.print(delimiter);
		//out.printf(doubleFormat + "\n",rum.theta);
		out.println(rum.theta);

		out.print("beta_PS");
		out.print(delimiter);
		out.println(rum.betaPS);
		
		
		
		out.print("Epsilon");
		out.print(delimiter);
		out.println(epsilon);

		int dForUniversalChoiceSetAlg = getDToUseInUniversalChoiceSetAlg();
		out.print("d-from-MSA");
		out.print(delimiter);
		out.println(dForUniversalChoiceSetAlg);

		out.print("Universal-choice-set-cutoff-factor");
		out.print(delimiter);
		out.println(maximumCostRatio);
		
		out.print("Local-Detour-factor");
		out.print(delimiter);
		out.println(Network.localMaximumCostRatio);
		
		out.print("Local-detour-cutoff-factor");
		out.print(delimiter);
		out.println(Network.localMaximumCostRatioCutOff);
		
		/*out.print("Detourparameter");
		out.print(delimiter);
		out.println(localMaximumCostRatio);
*/
		out.print("demandscalefactor");
		out.print(delimiter);
		out.println(demandScale);
		
		out.print("ConstrainedEnumerationEachIte");
		out.print(delimiter);
		out.println(consEnumIte);

		out.print("betaLength");
		out.print(delimiter);
		out.println(rum.betaLength);
		

		out.print("betaTime");
		out.print(delimiter);
		out.println(rum.betaTime);
		out.close();
		
		System.out.println("Parameters were successfully output to " + file.getName() + ".");
	}

	public void printParamToFile(String filename) {
		try {
			File file = new File(filename);
			printParamToFile(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Prints gap measures in the RSUE
	 * @see RSUET#columnGenerationAlgorithm(Network, String...)
	 * @param time the time to be printed
	 */
	private void printRSUETtime(double time) {
		System.out.println("RSUET converged successfully. Real time elapsed: " + time + " ms.");

	}

	/**
	 * used by the RSUET(phi,omega) algorithm where phi is 
	 * greater than "min".  
	 * to remove up to {@code maxNumberOfPathsToRemove} paths 
	 * and redistribute their flow acording to the the choice 
	 * probability of the remaining routes.
	 * 
	 * @see RSUET#solve(Network)
	 * @param maxNumberOfPathsToRemove the maximum number of routes that are removed
	 * @param network the network to perform redistribution on
	 */
	public void redistributeFlowOnMarkedRoutesAccordingToProbability(Network network,int maxNumberOfPathsToRemove) {
		for (HashMap<Integer,OD> m: network.ods.values()) { //for each OD
			for (OD od: m.values()) {
				/*if( network.useLocalStorage ){
					try {
						network.loadUniversalChoiceSetFromStorage(od);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}*/
				if (od.pathWasRemovedDuringLastIteration) {
					od.pathWasRemovedDuringLastIteration = false;
					continue;
				}
				double usedPathProbabilityMass = 0;
				double totalFlowToRedistribute = 0;
				//				First run: Calculate totals
				int numberOfRemovedPaths = 0;
				for (int i = 0; i < od.restrictedChoiceSet.size(); i++) {
					Path path = od.restrictedChoiceSet.get(i);
					if (path.p == 0 && path.getFlow() > 0 && numberOfRemovedPaths < maxNumberOfPathsToRemove) {
						path.markedForRemoval = true;
						od.pathWasRemovedDuringLastIteration = true;
						totalFlowToRedistribute += path.getFlow();
						numberOfRemovedPaths ++;
					} else {
						usedPathProbabilityMass += path.p;
						path.markedForRemoval = false;
					}
				}
				//				second run: redistribute
				for (Path path: od.restrictedChoiceSet) {
					if (path.markedForRemoval) {
						path.setFlow(0);
					} else {
						path.setFlow(path.getFlow()+path.p/usedPathProbabilityMass*totalFlowToRedistribute);
					}
				}
			}
		}
	}

	/**
	 * Alternative route removal method to 
	 * {@linkplain RSUET#redistributeFlowOnMarkedRoutesAccordingToProbability(Network, int)}.
	 * Not currently used.
	 * 
	 * @param network the network on which to perform the operation
	 * @param maxPercentFlowToRemove the maximum allowed share of the total demand on the route to remove. If 
	 * that the maximum amount is removed, an equal share is removed from all violating routes, relative 
	 * to their original flow. Otherwise, all flow on violating routes is removed.
	 */
	private void redistributeFlowOnMarkedRoutesMaxRelativeFlow(Network network, double maxPercentFlowToRemove) {
		if (!(maxPercentFlowToRemove >= 0 && maxPercentFlowToRemove <= 1)) throw new IllegalArgumentException("Argument must be between 0 and 1.");
		for (HashMap<Integer,OD> m: network.ods.values()) { //for each OD
			for (OD od: m.values()) {
				double totalFlowThatShouldBeThere = 0;
				double totalFlowThatShouldNotBeThere = 0;
				//				First run: Calculate totals
				HashSet<Integer> flaggedPaths = new HashSet<Integer>();
				for (int i = 0; i < od.restrictedChoiceSet.size(); i++) {
					Path path = od.restrictedChoiceSet.get(i);
					if (path.getAuxFlow() == 0) {
						totalFlowThatShouldNotBeThere += path.getFlow();
						flaggedPaths.add(i);
					} else {
						totalFlowThatShouldBeThere += path.getFlow();
					}
				}

				double totalFlowToRemove = Math.min(totalFlowThatShouldNotBeThere, maxPercentFlowToRemove*(totalFlowThatShouldBeThere+totalFlowThatShouldNotBeThere));

				if (totalFlowThatShouldNotBeThere == 0) continue;
				double percentToRemove = totalFlowToRemove / totalFlowThatShouldNotBeThere;
				double percentToAdd = totalFlowToRemove / totalFlowThatShouldBeThere;

				//				second run: redistribute
				for (Path path: od.restrictedChoiceSet) {
					if (path.getAuxFlow() == 0) {
						path.setFlow(path.getFlow()*(1-percentToRemove));
					} else {
						path.setFlow(path.getFlow()*(1+percentToAdd));
					}

				}
			}
		}
	}

	/**
	 * Removes all flow on only the most violating route and 
	 * redistributes it to the remaining routes according to 
	 * their choice probability. Not currently used.
	 * 
	 * @param network the network on which to perform the operation
	 */
	private void redistributeFlowOnMostViolatingRoute(Network network) {
		for (HashMap<Integer,OD> m: network.ods.values()) { //for each OD
			for (OD od: m.values()) {
				if (od.pathWasRemovedDuringLastIteration) {
					od.pathWasRemovedDuringLastIteration = false;
					continue;
				}
				double usedPathProbabilityMass = 0;
				double totalFlowToRedistribute = 0;
				//				First run: Calculate totals
				double costOfMostViolatingPath = 0;
				Path mostViolatingPath = null;
				for (int i = 0; i < od.restrictedChoiceSet.size(); i++) {
					Path path = od.restrictedChoiceSet.get(i);
					if (path.p == 0 && path.getFlow() > 0 && path.genCost > costOfMostViolatingPath) {
						mostViolatingPath = path;
					} else {
						usedPathProbabilityMass += path.p;
					}
				}
				if (mostViolatingPath == null) continue;
				totalFlowToRedistribute = mostViolatingPath.getFlow();
				//				second run: redistribute
				for (Path path: od.restrictedChoiceSet) {
					path.setFlow(path.getFlow()+path.p/usedPathProbabilityMass*totalFlowToRedistribute);
				}

				mostViolatingPath.setFlow(0);
			}
		}
	}


	/**
	 * This is the relative gap on unused routes above the threshold for
	 *  TMNL as defined in the working paper by Thomas Kjær Rasmussen.
	 * @param network the network on which to evaluate the gap from equilibrium due
	 * to unused routes
	 * @return the gap on unused routes
	 */
	public double relGapAbove(Network network) {
		double enumerator = 0;
		double denominator = 0;

		for (HashMap<Integer,OD> m: network.ods.values()) {
			for (OD od: m.values()) {
				double cmin = od.getMinimumCost();
				double delta = omega.calculateRefCost(od) - cmin;
				for (Path path: od.restrictedChoiceSet) {
					double pseudoCost = Math.max(0, path.genCost - cmin - delta);
					double flow = path.getFlow();
					enumerator += flow * pseudoCost;
					denominator += flow*path.genCost;
				}
			}
		}
		return enumerator/denominator;
	}

	/**
	 * This is the relative gap on unused routes below the threshold for TMNL as defined in the working
	 * paper by Thomas Kjær Rasmussen.
	 * @param network the network on which to evaluate the gap from equilibrium due
	 * to unused routes
	 * @return the gap on unused routes
	 */
	public double relGapBelow(Network network) {
		double enumerator = 0;
		double denominator = 0;

		for (HashMap<Integer,OD> m: network.ods.values()) {
			for (OD od: m.values()) {
				double cmin = od.getMinimumCost();
				double threshold = omega.calculateRefCost(od);
				double delta = threshold - cmin;
				double maxPseudoCost = 0;
				double demand = od.demand;
				for (Path path: od.restrictedChoiceSet) {
					double flow = path.getFlow();
					if (flow == 0) {
						double cost = path.genCost;
						double pseudoCost = Math.max(0, threshold - cost);
						if (pseudoCost > maxPseudoCost) maxPseudoCost = pseudoCost;
					}
					enumerator += demand * maxPseudoCost;
					denominator += delta * demand;
				}
			}
		}
		return enumerator/denominator;
	}

	@Override
	public ConvergencePattern solve(Network network) throws IOException {
		if (phi instanceof RefCostMin) return columnGenerationAlgorithm(network);
		else {
			return cutUniversalChoiceSetAlgorithm(network);
		}
	}

}
