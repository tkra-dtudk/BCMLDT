package network;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Locale;
import java.util.Scanner;

import auxiliary.ConvergencePattern;
import auxiliary.MyMinPriorityQueue;
import auxiliary.StdDraw;
import choiceModel.PSL;
import choiceModel.RSUET;
import choiceModel.RUM;
import choiceModel.RouteChoiceModel;
import refCostFun.RefCostFun;

/**
 * The {@code Network} class first of all serves as a network
 * representation constituted of sets of Nodes, Edges and OD
 * (Origin-Destination) pairs. Second of all, it contains
 * methods meant to perform operations to illustrate the 
 * network, update edge LoS, perform all-or-nothing assignment,
 * print network information to files, and more. 
 * <p>
 * As a rule of thumb, methods that represent algorithms beyond
 * basic ones such as Dijkstra's algorithm, should not be contained
 * in this class, but rather in a different model object, such as 
 * the {@link RSUET}, which is an extension of the abstract class
 * {@linkplain RouteChoiceModel}.
 * 
 * @author mesch
 * @see Edge
 * @see Node
 * @see OD
 * @see RouteChoiceModel
 */
public class Network {

	public char delim = ';';
	public long  totalNumberOfPaths = 0;
	public long  totalNumberOfNodesInPaths = 0;
	public int OCounter = 0;
	public double maximumCostRatio = 50;
	public double maximumCostDeviation = 10000;
	public double detourednessParameter = 0;
	public boolean universalChoiceSetsStored = false;
	public boolean isNetworkBidirectional;
	public boolean printStatusOnTheGo;
	public double demandScale;


	public Boolean useLocalStorage;
	private String localStorageDirectory;

	public double getDetourednessParameter() {
		return this.detourednessParameter;
	}
	public void setDetourednessParameter(double value) {
		this.detourednessParameter = value;
	}

	public void setLocalStorageDirectory(String localStorageDirectory){
		this.localStorageDirectory = localStorageDirectory;
	}

	public void setIsNetworkBirectional(boolean isNetworkBirectional){
		this.isNetworkBidirectional = isNetworkBirectional;
	}

	public void setUseLocalStorage(Boolean useLocalStorage){
		this.useLocalStorage = useLocalStorage;
	}


	/**  
	 * Name of the network.
	 */
	private String networkName;

	/**  
	 * For valid paths the distance between two nodes,
	 * can at most be localMaximumCostRatio larger than
	 * the shortest path between these two nodes.
	 * @see Network#setLocalMaximumCostRatioCutOff(double)
	 */
	public static double localMaximumCostRatioCutOff;

	public static double localMaximumCostRatio;


	/**  
	 * For a shortest path tree of any node in the network,
	 * for all other nodes the id of the previous node in
	 * the shortest path to the destination is stored in
	 * this HashMap.
	 * @see Network#generateAllShortestPathTrees()
	 */
	public HashMap<Integer,HashMap<Integer,Integer>> dijkstraPrevs = new HashMap<Integer,HashMap<Integer,Integer>>();

	/**  
	 * For a shortest path tree of any node in the network,
	 * the shortest distance from all other nodes is stored
	 * in this HashMap.
	 * @see Network#generateAllShortestPathTrees()
	 */
	public HashMap<Integer,HashMap<Integer,Double>> dijkstraDists = new HashMap<Integer,HashMap<Integer,Double>>();

	public double minimumFlowToBeConsideredUsed = 0;


	/**
	 * This hashmap indexes the set of network nodes by
	 * their integer ID.
	 */
	HashMap<Integer,Node> nodes; 

	/**
	 * Set of edges, implemented as an array. This structure
	 * requires that edges be indexed from 1 to N, where N
	 * is the number of edges in the network. Could easily, 
	 * and probably should, be implemented as a HashMap. 
	 * <p>
	 * Is now made redundant with {@code edgesNodePair}; should 
	 * be deprecated and phased out.
	 * @see Network#getNode(int)
	 * @see Network#edgesNodePair
	 */
	private HashMap<Integer,Edge> edges; // Set of edges
	/**
	 * Edges identified by their tail and head, which is not 
	 * only preferable to having a dedicated set of edge
	 * indices, but also more computationally efficient
	 * when recalling an edge from node i to j, and more
	 * convenient. Should replace {@code edges} completely.
	 * 
	 *  @see Network#edges
	 */
	HashMap<Integer,HashMap<Integer,Edge>> edgesNodePair = new HashMap<Integer,HashMap<Integer,Edge>>(); //set of node pairs

	/**
	 * Set of Origin-destination relations, so that {@code ods.get(i).get(j)}
	 * gets the OD that goes from node {@code i} to {@code j}.
	 */
	public HashMap<Integer, HashMap<Integer,OD>> ods; // Set of ODs

	/**
	 * "general" big-M, but it is cleaner to implement this as a local
	 * variable in each algorithm.
	 * @deprecated
	 */
	private double M = Double.POSITIVE_INFINITY; // Big M for use in dijkstra's algorithm
	/**
	 * The design intent is that this keeps track of when the universal
	 * choice set is correctly stored in OD.R, but is is clumsy 
	 * and should maybe be implemented differently.
	 */
	private boolean isUniversalChoiceSetsGenerated = false;
	private int numOD = 0;
	public boolean printCS;

	public Network(String folderName){
		new Network(folderName, false, 1.0);
	}

	/**
	 * Very important constructor. This does not only read
	 * the network data, it also pre-processes the network. 
	 * 
	 * The current implementation is robust, but also inefficient,
	 * which can be a slight nuisance when reading large networks
	 * from virtual drives, since there are so many data that occur
	 * during file read. Reading from a duplicate local folder
	 * should improve performance significantly.
	 * @param folderName the name of the folder to which to write 
	 * the output file
	 *
	 */
	public Network(String folderName, boolean isNetworkBirectional, double demandScale) {
		setIsNetworkBirectional(isNetworkBirectional);
		this.demandScale=demandScale;
		networkName = folderName.substring(folderName.lastIndexOf("/") + 1).trim();
		File file = new File(folderName);
		if (file.isDirectory()) {
			File[] dirContents = file.listFiles();
			boolean isNetFilePresent = false;
			boolean isNodeFilePresent = false;
			boolean isTripsFilePresent = false;
			String fileName1 = "";
			String fileName2 = "";
			String fileName3 = "";
			//			This is added to the end of the folder specification if it has not already been provided
			String addThis = "/";
			if (file.getName().endsWith("/")) {
				addThis = "";
			}
			for (int i = 0; i < dirContents.length; i++) {
				String thisFileName = dirContents[i].getName();
				if (thisFileName.endsWith("_net.tntp")) {
					isNetFilePresent = true;
					fileName1 = folderName + addThis + thisFileName;
				} else if (thisFileName.endsWith("_node.tntp")) {
					isNodeFilePresent = true;
					fileName2 = folderName + addThis + thisFileName;
				} else if (thisFileName.endsWith("_trips.tntp")) {
					isTripsFilePresent = true;
					fileName3 = folderName + addThis + thisFileName;
				}

			}
			if (!isNetFilePresent) {
				throwNetworkReadExceptionFileNotFound("_net.tntp");
			} else if (!isTripsFilePresent) {
				throwNetworkReadExceptionFileNotFound("_node.tntp");
			} else if (!isNodeFilePresent) {
				System.err.println("Warning! No node file provided. Cannot draw network. Proceeding with artificial node data.");
			}

			readNetworkRaw(fileName1, fileName2, fileName3);
		} else {
			throw new InputMismatchException("Input string must point to a directory of network files.");
		}
	}

	/**
	 * Constructs a new path object and adds it
	 * to the restricted choice set.
	 * 
	 * @param od the OD relation of the path to be added
	 * @param newNodeSeq the node sequence of the path to be added
	 * as an array of integers
	 * @see Network#generateUniversalChoiceSets()
	 */
	private void addPathToUniversalChoiceSet(OD od, int[] newNodeSeq, double relativeSubtourLength) {
		Path addThisPath = new Path(nodeSeqAsEdgeList(newNodeSeq),od, relativeSubtourLength);
		addThisPath.nodeSeq = newNodeSeq;
		addThisPath.network = this;
		od.R.add(addThisPath);
	}

	/**
	 * Performs an all-or-nothing assignment using the current
	 * path costs, as defined by {@link Path#genCost}. This also 
	 * adds the shortest paths to the restricted choice sets of
	 * the appropriate OD-relation, {@link OD#restrictedChoiceSet}. 
	 * This is an important detail to keep in mind when using
	 * this in an algorithm such as the RSUET; if the behavior
	 * is unwanted, the paths can simply be overwritten by 
	 * re-initializing the restricted choice sets as empty
	 * ArrayList after calling this method.
	 * <p>
	 * Also keep in mind that the all-or-nothing assignment 
	 * does NOT include network loading -- the paths are
	 * only assigned flow, but loaded onto the network.
	 * This should be achieved by manually calling 
	 * {@link Network#loadNetwork()}.
	 * 
	 */
	public void allOrNothing() {
		int lastOrigin = -3;
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) {// For each OD
				if (od.O != lastOrigin) {
					//OD pair are sorted by O first. Dijkstra is required once for each origin with demand from it. 
					dijkstraMinPriorityQueue(this.getNode(od.O));
				}
				lastOrigin = od.O;
				Path path = shortestPath(od);
				od.addPath(path);// Add shortest path to choice set
				path.setFlow(od.demand); // Assign all traffic to shortest path
			}
		}
	}

	/**
	 * Calculates the average size of the restricted choice sets,
	 * weighting each OD-relation equally. The size of each 
	 * choice set is the number of saved paths  which have a
	 * flow of strictly more than 0.
	 * 
	 * @return the average restricted choice set size.
	 */
	public double calculateAvgChoiceSetSize() {
		double numUsedRoutesTotal = 0;
		double numOds = 0;
		for (HashMap<Integer,OD> m: ods.values()) {
			for (OD od: m.values()) {
				numOds ++;
				for (Path path: od.restrictedChoiceSet) {
					if (path.getFlow() > 0) {
						numUsedRoutesTotal++;
					}
				}
			}
		}
		return numUsedRoutesTotal/numOds;
	}

	/**
	 * Calculates the number of OD-relations in the network,  
	 * meaning those where demand is above 0; this corresponds
	 * to the number of objects in {@linkplain Network#ods}.
	 * 
	 * @return the number of OD-relations
	 */
	public int calculateNumOd() {
		int sum = 0;
		for (HashMap<Integer,OD> m: ods.values()) {
			sum += m.size();
		}
		return sum;
	}

	/**
	 * Calculates the sum of demand over OD-relations
	 * on the network. 
	 * 
	 * @return the total demand
	 */
	public double calculateTotalDemand() {
		double sum = 0;
		for (HashMap<Integer,OD> m: ods.values()) {
			for (OD od: m.values()) {
				sum += od.demand;
			}
		}
		return sum;
	}

	/**
	 * For each OD, finds the shortest path and 
	 * adds it to the restricted chocie set if 
	 * it is not already part of it.
	 * @return true if successful, false otherwise
	 */
	public boolean columnGeneration() {
		Path path;
		int previousOD = -3;
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				if (od.O != previousOD) {
					dijkstraMinPriorityQueue(this.getNode(od.O));
				}
				previousOD = od.O;

				path = shortestPath(od);
				// If current shortest path is not already in the choice set, add it
				boolean alreadyInChoiceSet = false;
				for (int j = 0; j < od.restrictedChoiceSet.size(); j++) {
					if (path.equals(od.restrictedChoiceSet.get(j))) {
						alreadyInChoiceSet = true;
						break;
					}
				}
				if (!alreadyInChoiceSet) {
					od.addPath(path);
					path.updateCost(detourednessParameter);
					if (od.getMinimumCost() > path.genCost) od.setMinimumCost(path.genCost);
					od.pathWasAddedDuringColumnGeneration = true;
				}
			}
		}
		return true;
	}

	/**
	 * The universal choice set usually includes many irrelevant paths. 
	 * To increase computational performance, these may be excluded by
	 * defining some cost above which paths will be removed, hoping
	 * that all relevant routes are still considered. 
	 * <p>
	 * This method cuts off all paths from  the universal choice set
	 * which have a cost equal  to or greater than {@code maximumCostRatio} 
	 * times the shortest path cost at the time of the method call.
	 * 
	 * @param maximumCostRatio the factor so that paths od.R where
	 * {@code maximumCostRatio * od.getMinimumCost() >= path.genCost}
	 * are removed.
	 */
	public void cutUniversalChoiceSets(double maximumCostRatio) {
		updateUniversalChoiceSetCosts();
		if (maximumCostRatio == -1) {
			for (HashMap<Integer,OD> m: ods.values()) {
				for (OD od: m.values()) {
					od.restrictedChoiceSet = od.R;
					od.R = null; //erase universal choice set for safety reasons
				}
			}
			isUniversalChoiceSetsGenerated = false;
			return;
		}//else

		for (HashMap<Integer,OD> m: ods.values()) {
			for (OD od: m.values()) {
				if(useLocalStorage){
					try {
						loadUniversalChoiceSetFromStorage(od);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Collections.sort(od.R);
				od.setMinimumCost(od.R.get(0).genCost);
				double maximumCost = maximumCostRatio * od.getMinimumCost();
				od.restrictedChoiceSet = new ArrayList<Path>();
				for (Path path: od.R) {
					if (path.genCost <= maximumCost) {
						od.restrictedChoiceSet.add(path);
					} else break;
				}
				if(useLocalStorage){
					try {
						transferUniversalChoiceSetToStorage(od);
					} catch (IOException e) {
						e.printStackTrace();
					}
					od.R.clear();
				}
			}
		}

	}

	/**
	 * An efficient implementation of the dijkstra algorithm using a augmented
	 * min-priority queue with a mapped binary heap. Also includes early
	 * termination, i.e. when all destinations from the node have been
	 * solved, the algorithm stops. Uses relatively a lot of memory, but 
	 * this is usually a minor issue. 
	 * 
	 * @param originNode the node from which to find the shortest path to 
	 * all destinations with demand from the node. 
	 * @return true if success, false otherwise
	 */
	public boolean dijkstraMinPriorityQueue(Node originNode) {
		int originNodeID = originNode.getId();
		//Initialization

		MyMinPriorityQueue<Node> Q = new MyMinPriorityQueue<Node>();
		HashSet<Integer> Qbuddy = new HashSet<Integer>(this.getNumNodes()/3); //default initial capacity is one third of network size
		HashSet<Integer> destinations = new HashSet<Integer>();

		initializeDijkstraMinPriorityQueue(Q, Qbuddy, destinations, originNodeID, originNode);

		while (!destinations.isEmpty()) {
			//This is the neat thing about the priority queue. "polling" takes out the minimum element (the one with lowest distance) 
			Node u = Q.poll();
			destinations.remove(u.getId());
			u.dijkstraVisitied = true;

			for (int vID: u.getNeighbours()) {
				Node v = this.getNode(vID);
				if (v.dijkstraVisitied) {
					continue;
				}
				if (!Qbuddy.contains(vID)) {
					Qbuddy.add(vID);
					Q.add(v);
				}
				double alt = u.dijkstraDist + this.getEdge(u,v).getGenCost(); 
				if (alt < v.dijkstraDist) {
					v.dijkstraDist = alt;
					v.dijkstraPrev = u;

					//	Q.remove(v);
					//	Q.add(v);
					//Updating in the priority queue and sifting up is now efficiently (O(log(n))) 
					//implemented in {@code MyMinPriorityQueue} with a hashmap by @author mesch
					Q.updateHeapPositionUp(v);
				}
			}
		}

		return true;
	}

	public void dijkstraMinPriorityQueueWithStorage(Node originNode) {
		dijkstraMinPriorityQueue(originNode);
		int id = originNode.getId();
		dijkstraPrevs.put(id,new HashMap<Integer,Integer>());
		dijkstraDists.put(id,new HashMap<Integer,Double>());
		for(Node v : nodes.values()){
//			System.out.println(id + "Origin " + originNode.getId() + " network node " + v.getId());
			dijkstraPrevs.get(id).put(v.getId(), v.dijkstraPrev.getId());
			dijkstraDists.get(id).put(v.getId(), v.dijkstraDist);
		}
	}

	/**
	 * Draws this network with StdDraw using Node coordinates.
	 * 
	 * @param type
	 *            determines which labels are applied to the edges:
	 *            <ul>
	 *            <li>type = 0: Free flow time.</li>
	 *            <li>type = 1: Congested time (total time).</li>
	 *            <li>type = 2: Ratio of total time to free flow time.</li>
	 *            <li>type = 3: Flow.</li>
	 *            <li>type = 4: Free flow time / total time.</li>
	 *            </ul>
	 * @param canvasSize
	 *            the size of the square drawing, measured in pixels
	 */
	public void draw(int type, int canvasSize) { // Draw network
		// Calculate maximum coordinates
		double maxX = 0;
		double maxY = 0;
		for (Node node: nodes.values()) {
			maxX = Math.max(maxX, node.getX());
			maxY = Math.max(maxY, node.getY());
		}
		double L = 1.2 * Math.max(maxX, maxY);
		StdDraw.setCanvasSize(canvasSize, canvasSize);
		StdDraw.setScale(0, L);
		int fontSize = canvasSize / 70;
		Font font = new Font("Arial", 0, fontSize);
		StdDraw.setFont(font);
		double circleRadius = L / 100;

		// Draw nodes
		for (Node node: nodes.values()) {
			StdDraw.circle(node.getX(), node.getY(), circleRadius);
			StdDraw.text(node.getX(), node.getY(), node.getId() + "");
		}

		Node fromNode;
		Node toNode;
		// Draw edges
		for (Edge edge : edges.values()) {
			fromNode = this.getNode(edge.getTail());
			toNode = this.getNode(edge.getHead());
			drawEdge(fromNode, toNode, circleRadius, type);
		}
	}

	/**
	 * private method used by {@code draw} to draw edges
	 * @param from from node
	 * @param to to node
	 * @param circleRadius radius of the line used
	 * @param type integer specifying the label type,
	 * see {@link Network#draw(int, int)}
	 */
	private void drawEdge(Node from, Node to, double circleRadius, int type) {
		double x0 = from.getX();
		double y0 = from.getY();
		double x1 = to.getX();
		double y1 = to.getY();
		double xDist = x1 - x0;
		double yDist = y1 - y0;
		double dist = Math.pow(Math.pow(xDist, 2) + Math.pow(yDist, 2), 0.5); // Pythagoras
		double rx = xDist / dist;
		double ry = yDist / dist;

		// Readjust to start line outside circle
		x0 += rx * circleRadius * 1.2;
		y0 += ry * circleRadius * 1.2;
		x1 -= rx * circleRadius * 1.2;
		y1 -= ry * circleRadius * 1.2;

		// Readjust to offset lines
		// orthogonal directional vector to (rx,ry) is (-ry,rx)
		x0 += -ry * circleRadius * 0.9;
		y0 += rx * circleRadius * 0.9;
		x1 += -ry * circleRadius * 0.9;
		y1 += rx * circleRadius * 0.9;

		StdDraw.line(x0, y0, x1, y1);
		double[] xCords = { x1, x1 - 2 * circleRadius * rx - circleRadius * ry,
				x1 - 2 * circleRadius * rx + circleRadius * ry };
		double[] yCords = { y1, y1 - 2 * circleRadius * ry + circleRadius * rx,
				y1 - 2 * circleRadius * ry - circleRadius * rx };
		StdDraw.filledPolygon(xCords, yCords);

		// Draw edge length in middle of arrow, offset to the "left"
		double x3 = x0 + (x1 - x0) / 2;
		double y3 = y0 + (y1 - y0) / 2;
		x3 = x3 - ry * 1.1 * circleRadius;
		y3 = y3 + rx * 1.1 * circleRadius;
		String drawThis = "";
		if (type == 0) {// freeFlowTime
			drawThis = String.format("%.1f", getEdge(from, to).getFreeFlowTime());
		} else if (type == 1) {// Draw time
			drawThis = String.format("%.1f", getEdge(from, to).getTime());
		} else if (type == 2) {// draw time/freeFlowTime
			drawThis = String.format("%.3f", getEdge(from, to).getTime() / getEdge(from, to).getFreeFlowTime());
		} else if (type == 3) {
			drawThis = String.format("%.0f", getEdge(from, to).getFlow());
		} else if (type == 4) {
			drawThis = String.format("%.0f/%.1f", getEdge(from, to).getFreeFlowTime(), getEdge(from, to).getTime());
		} else if (type == -1) {
			// Print no text
		}

		StdDraw.text(x3, y3, drawThis);// , Math.toDegrees(Math.atan(ry/rx)));
	}

	/**
	 * Private method used by {@linkplain Network#draw(int, int)} to draw
	 * nodes.
	 * @param node the node to draw
	 * @param circleRadius radius of the node circle outline
	 */
	private void drawNode(Node node, double circleRadius) {
		StdDraw.setPenColor(StdDraw.BLACK);
		StdDraw.setPenRadius();
		StdDraw.setPenRadius(StdDraw.getPenRadius() * 2.5);
		StdDraw.circle(node.getX(), node.getY(), circleRadius * 1.1);
	}

	/**
	 * Highlights a set of edges on a canvas after a 
	 * call of {@linkplain Network#draw(int, int)}. 
	 * @param path the path to highlight
	 * @param type
	 *            determines which labels are applied to the edges:
	 *            <ul>
	 *            <li>type = 0: Free flow time.</li>
	 *            <li>type = 1: Congested time (total time).</li>
	 *            <li>type = 2: Ratio of total time to free flow time.</li>
	 *            <li>type = 3: Flow.</li>
	 *            <li>type = 4: Free flow time / total time.</li>
	 *            </ul>
	 * @param circleRadius the radius to which the line thickness is scaled.
	 */
	public void drawPath(Path path, int type, double circleRadius) {
		StdDraw.setPenRadius();
		StdDraw.setPenRadius(StdDraw.getPenRadius() * 2);// Two times default
		// pen size (i.e.
		// line thickness)
		Node from;
		Node to;
		for (int i = 0; i < path.edges.size(); i++) {
			from = this.getNode(path.edges.get(i).getTail());
			to = this.getNode(path.edges.get(i).getHead());
			drawEdge(from, to, circleRadius, type);
		}
		StdDraw.setPenColor(StdDraw.BLACK);
		StdDraw.setPenRadius();

		// Draw origin and destination nodes
		drawNode(this.getNode(path.getO()), circleRadius);
		drawNode(this.getNode(path.getD()), circleRadius);
	}


	/** 
	 * Generates the shortest path tree for every
	 * node of the network. In each of these cases
	 * the distance to all other nodes is stored in
	 * dijkstraDists
	 */
	public void generateAllShortestPathTrees(){
		for (Node origin : nodes.values()){
			dijkstraMinPriorityQueueWithStorage(origin);
		}
	}


	/** 
	 * Fills out the intial restricted choice sets
	 * by calling the recursive function {@code minos} for all 
	 * OD relations. A locally and globally constrained path
	 * enumeration is used to produce the choice sets.
	 * @throws IOException 
	 */
	public void generateUniversalChoiceSets() throws IOException {  //this is used by local detour.
		if (isUniversalChoiceSetsGenerated) return;
		System.out.println("Generating initial restricted choice set...");
		int u;
		int[] currentPath;
		double lengthOfCurrentPath;
		boolean[] unvisited;
		int numNodes = getNumNodes();
		generateAllShortestPathTrees();
		long start = System.currentTimeMillis();
		long tempTimer = System.currentTimeMillis();
		for (HashMap<Integer, OD> m: ods.values()) {
			OCounter++;
			System.out.println("Origin #" + OCounter + " of " + ods.size() + " is being processed.");
			int DCounter = 0;
			for (OD od: m.values()) { // For each OD-pair; "minos" works on the OD-level
				DCounter++;

//				if(maximumCostDeviation==-1) {
//					double maximumToleratedPathCostFromOtoD = dijkstraDists.get(od.O).get(od.D) * maximumCostRatio;
//				}
				//double maximumToleratedPathCostFromOtoD = dijkstraDists.get(od.O).get(od.D) * maximumCostRatio;
				
				double maximumToleratedPathCostFromOtoD = dijkstraDists.get(od.O).get(od.D) + maximumCostDeviation;
				if(maximumCostDeviation==-1) {
					maximumToleratedPathCostFromOtoD = dijkstraDists.get(od.O).get(od.D) * maximumCostRatio;
					System.out.println("dijkstradist: " + dijkstraDists.get(od.O).get(od.D));
				}
				
				if(printStatusOnTheGo){
					System.out.print("     Destination #" + DCounter + " of " + m.size() + " is being processed.");
					System.out.print(" Maximum cost is " + maximumToleratedPathCostFromOtoD + ".");
				}
				od.R = new ArrayList<Path>();
				currentPath = new int[1];
				u = od.O; // Recursion starts in the origin node
				currentPath[0] = u; //u er første knude i current path
				lengthOfCurrentPath = 0; //omkostning af current path

				unvisited = new boolean[numNodes]; 
				Arrays.fill(unvisited, true);
				unvisited[u - 1] = false; // Origin node starts out as visited, all other unvisited yet. noder har formentlig navne fra 1-N, ikke 0 - N-1

				minos(od, u, currentPath, lengthOfCurrentPath, unvisited, maximumToleratedPathCostFromOtoD, 0.);

				if(useLocalStorage){
					transferUniversalChoiceSetToStorage(od);
					od.R.clear();
				}
				if(printStatusOnTheGo){
					System.out.print(" Total n.o. paths: " + totalNumberOfPaths);
					System.out.print(" Total n.o. nodes in paths: " + totalNumberOfNodesInPaths + ". Calculation time OD: " + (System.currentTimeMillis() - tempTimer)/1000d);
					System.out.println(". Free memory (mb): " + Runtime.getRuntime().freeMemory()/1048576  + "."); 
				}
				tempTimer = System.currentTimeMillis();
			}
		}
		//		updateUniversalDeltas(); // Updates "deltaUniversal" for use in Pathsize
		// Factor calculation
		System.out.println((System.currentTimeMillis() - start)/1000d);
		System.out.println("Universal choice set successfully generated.");
		isUniversalChoiceSetsGenerated = true;
	}

	public void consEnum(double bound) throws IOException {
		System.out.println("Generating constrained Enumeration Choice Set...");
		int u;
		int[] currentPath;
		double lengthOfCurrentPath;
		boolean[] unvisited;
		int numNodes = getNumNodes();
		generateAllShortestPathTrees();
		long start = System.currentTimeMillis();
		long tempTimer = System.currentTimeMillis();
		for (HashMap<Integer, OD> m: ods.values()) {
			OCounter++;
			//System.out.println("Origin #" + OCounter + " of " + ods.size() + " is being processed.");
			int DCounter = 0;
			for (OD odReal: m.values()) { // For each OD-pair; "minos" works on the OD-level
				OD od = new OD(odReal.O,odReal.D,odReal.demand);
				DCounter++;
				double maximumToleratedPathCostFromOtoD = dijkstraDists.get(od.O).get(od.D) + bound;
				if(printStatusOnTheGo){
					System.out.print("     Destination #" + DCounter + " of " + m.size() + " is being processed.");
					System.out.print(" Maximum cost is " + maximumToleratedPathCostFromOtoD + ".");
				}
				od.R = new ArrayList<Path>();
				currentPath = new int[1];
				u = od.O; // Recursion starts in the origin node
				currentPath[0] = u;
				lengthOfCurrentPath = 0;

				unvisited = new boolean[numNodes];
				Arrays.fill(unvisited, true);
				unvisited[u - 1] = false; // Origin node starts out as visited

				minos(od, u, currentPath, lengthOfCurrentPath, unvisited, maximumToleratedPathCostFromOtoD, 0d);

				if(useLocalStorage){
					transferUniversalChoiceSetToStorage(od);
					od.R.clear();
				}
				if(printStatusOnTheGo){
					System.out.print(" Total n.o. paths: " + totalNumberOfPaths);
					System.out.print(" Total n.o. nodes in paths: " + totalNumberOfNodesInPaths + ". Calculation time OD: " + (System.currentTimeMillis() - tempTimer)/1000d);
					System.out.println(". Free memory (mb): " + Runtime.getRuntime().freeMemory()/1048576  + "."); 
				}
				tempTimer = System.currentTimeMillis();
			}
		}
		//		updateUniversalDeltas(); // Updates "deltaUniversal" for use in Pathsize
		// Factor calculation
		System.out.println((System.currentTimeMillis() - start)/1000d);
		System.out.println("Universal choice set successfully generated.");
		isUniversalChoiceSetsGenerated = true;
	}
	/**
	 * Transfers the restricted choice set to the local
	 * storage directory. This is done to reduce the
	 * requirements of available memory.
	 */
	private void transferUniversalChoiceSetToStorage(OD od) throws IOException{
		FileWriter writer = new FileWriter(localStorageDirectory + "PathsO" + od.O + "D" + od.D + ".csv");
		for(int i = 0; i < od.R.size(); i++){
			Path path = od.R.get(i);
			writer.append(String.valueOf(path.getFlow()) + delim + String.valueOf(path.getAuxFlow()) + delim +
					String.valueOf(path.length) + delim + String.valueOf(path.genCost) + delim +
					String.valueOf(path.enumeratorInProbabilityExpression) + delim + String.valueOf(path.p) + delim +
					String.valueOf(path.transformedCost) + delim + String.valueOf(path.PS) + delim +
					String.valueOf(path.markedForRemoval) + delim);
			if(!path.edges.isEmpty()){
				for(int j = 0; j < path.edges.size() -1; j++){
					writer.append( String.valueOf(path.edges.get(j).getId()) + delim);
				}
				writer.append(String.valueOf(path.edges.get(path.edges.size()-1).getId()) + "\n" );
			}
		}

		writer.flush();
		writer.close();
	}

	public void exportConsideredPaths() throws IOException {
		for (HashMap<Integer, OD> m: ods.values()) {
			for(OD od : m.values()) {
				exportConsideredPaths(od);
			}
		}
	}

	private void exportConsideredPaths(OD od) throws IOException{
		FileWriter writer = new FileWriter(localStorageDirectory + "PathsO" + od.O + "D" + od.D + "_Considered.csv");
		for(int i = 0; i < od.R.size(); i++){
			Path path = od.R.get(i);
			if(path.getHasBeenUsed()){
				writer.append(String.valueOf(0) + delim + String.valueOf(0) + delim +
						String.valueOf(path.length) + delim + String.valueOf(path.genCost) + delim +
						String.valueOf(path.enumeratorInProbabilityExpression) + delim + String.valueOf(path.p) + delim +
						String.valueOf(path.transformedCost) + delim + String.valueOf(path.PS) + delim +
						String.valueOf(path.markedForRemoval) + delim);
				if(!path.edges.isEmpty()){
					for(int j = 0; j < path.edges.size() -1; j++){
						writer.append( String.valueOf(path.edges.get(j).getId()) + delim);
					}
					writer.append(String.valueOf(path.edges.get(path.edges.size()-1).getId()) + "\n" );
				}
			}
		}
		writer.flush();
		writer.close();
	}





	public void loadUniversalChoiceSetFromStorage(OD od) throws IOException {
		loadUniversalChoiceSetFromStorage(od,localStorageDirectory + "PathsO" + od.O + "D" + od.D + ".csv");
	}

	public void loadConsideredPaths(OD od) throws IOException {
		loadUniversalChoiceSetFromStorage(od,localStorageDirectory + "PathsO" + od.O + "D" + od.D + "_Considered.csv");
	}

	public void loadConsideredPaths() throws IOException {
		for (HashMap<Integer, OD> m: ods.values()) {
			for(OD od : m.values()) {
				loadConsideredPaths(od);
			}
		}
	}

	/**
	 * Loads restricted choice sets from storage.
	 */
	public void loadUniversalChoiceSetFromStorage(OD od, String outfile) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(outfile));
		String readLine;
		while( (readLine = br.readLine()) != null){
			Path path = new Path(new ArrayList<Edge>(), od);
			int loadCounter = 0;
			String tempString = "";
			for(int i = 0; i < readLine.length(); i++){
				char ch = readLine.charAt(i);
				if ( ch == delim){
					loadCounter++;
					switch(loadCounter){
					case 1: path.setFlow(Double.valueOf(tempString)); break;
					case 2: path.setAuxFlow(Double.valueOf(tempString)); break;
					case 3: path.length = Double.valueOf(tempString); break;
					case 4: path.genCost = Double.valueOf(tempString); break;
					case 5: path.enumeratorInProbabilityExpression = Double.valueOf(tempString); break;
					case 6: path.p = Double.valueOf(tempString); break;
					case 7: path.transformedCost = Double.valueOf(tempString); break;
					case 8: path.PS = Double.valueOf(tempString); break;
					case 9: path.markedForRemoval = Boolean.valueOf(tempString); break;
					default: path.edges.add(getEdge(Integer.valueOf(tempString))); break;
					}
					tempString = "";
				} else {
					tempString += ch;
				}
			}
			path.edges.add(getEdge(Integer.valueOf(tempString)));
			path.updateCost(detourednessParameter);
			od.R.add(path);
		}
	}

	/**
	 * Gets an edge by its single integer ID; this method should
	 * be phased out in favour of strictly adhering to the structure
	 * of {@linkplain Network#edgesNodePair}, where edges are identified 
	 * by head- and tail nodes.
	 * @param edgeID The single integer ID of the edge; this is implicitly specified
	 * by the order in which edges are read from the _net.tntp file.
	 * @return the edge with ID edgeID. 
	 */
	public Edge getEdge(int edgeId){
		if(edgeId < 0){
			Edge edge = edges.get(-edgeId);
			return edgesNodePair.get(edge.getHead()).get(edge.getTail());
		} else {
			Edge edge = edges.get(edgeId);
			return edgesNodePair.get(edge.getTail()).get(edge.getHead());
		} 		
	}

	/**
	 * This is the preferred way of recalling edges, replacing {@code getEdge(int)}.
	 * This operation is O(1).
	 * 
	 * @param tail the integer ID of the node from which the edge originates.
	 * @param head the integer ID of the node at which the edge terminates
	 * @return the edge
	 */
	public Edge getEdge(int tail, int head) {
		return edgesNodePair.get(tail).get(head);
	}

	/**
	 * This another acceptable way of recalling edges, replacing {@code getEdge(int)}; 
	 * slightly faster is the mapping by integer ID, see {@linkplain Network#getEdge(int, int)}.
	 * This operation is O(1).
	 * 
	 * @param tail the integer ID of the node from which the edge originates.
	 * @param head the integer ID of the node at which the edge terminates
	 * @return the edge
	 * @see Network#getEdge(int, int)
	 */
	public Edge getEdge(Node tail, Node head) {
		return getEdge(tail.getId(), head.getId());
	}

	/**
	 * Returns the name of the network.
	 * @return
	 */
	public String getNetworkName() {
		return this.networkName;
	}

	/**
	 * The preferred way of recalling the node 
	 * with ID {@code nodeID}. This operation is O(1).
	 * 
	 * @param nodeID the integer ID of the node to return
	 * @return the node with ID {@code nodeID}
	 */
	public Node getNode(int nodeID) {
		return nodes.get(nodeID);

	}

	/**
	 * 
	 * @return the number of edges in the network. 
	 * This number is retrieved from the length
	 * of the vector of edges.
	 */
	public int getNumEdges() {
		return edges.size();
	}

	/**
	 * 
	 * @return The number of nodes in the network.
	 * This is retrieved from the list of nodes.
	 */
	public int getNumNodes() {
		return nodes.size();
	}

	/**
	 * @return the number of OD pairs as was computed
	 * at the pre-processing of the network at object 
	 * construction
	 */
	public int getNumOD() {
		return this.numOD;
	}

	/**
	 * Retrieves an OD-relation in O(1) time.
	 * 
	 * @param O the integer ID of the origin node
	 * @param D the integer ID of the destination node
	 * @return the OD
	 */
	public OD getOD(int O, int D) {
		return ods.get(O).get(D);
	}

	/**
	 * Inititialization of the Dijkstra algorithm
	 * for improved brevity and readability. 
	 * 
	 * @param Q the set of visited nodes as a priority queue; the shortest
	 * path to these nodes has been found. 
	 * @param Qbuddy identical to Q, but as a HashMap to regularly 
	 * check what is in the queue
	 * @param destinations HashSet of integers corresponding to
	 * the list of still unvisited destinations with demand
	 * @param originNodeID integer ID of origin node
	 * @param originNode origin as node
	 */
	private void initializeDijkstraMinPriorityQueue(AbstractQueue<Node> Q, HashSet<Integer> Qbuddy, 
			HashSet<Integer> destinations,int originNodeID, Node originNode) {
		for (Node node: nodes.values()) {
			if (node.getId() != originNodeID){
				node.dijkstraDist = M;
				node.dijkstraPrev = null;
				node.dijkstraVisitied = false;
			} else {
				originNode.dijkstraDist = 0;
				node.dijkstraVisitied = true;
			}
		}

		//		Add only the origin node to the priority queue
		Q.add(originNode);
		Qbuddy.add(originNodeID);
		//	for (OD od : ods.get(originNodeID).values()) {
		//		destinations.add(od.D);
		//	}
		for(int nodeId : nodes.keySet()){
			if(nodeId != originNode.getId()){
				destinations.add(nodeId);
			}
		}
	}

	/**
	 * Makes the flow on edges in the network correspond
	 * to the flow on paths in restricted choice sets. 
	 * <p>
	 * Network loading can take relatively quite a long 
	 * time when the choice sets are large.
	 */
	public void loadNetwork() {
		// Reset link counts to 0
		for (Edge edge: edges.values()) {
			edge.setFlow(0);
		}

		// Load network path by adding flow path by path
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				for (Path path: od.restrictedChoiceSet) {
					path.load();
				}
			}
		}
	}

	/**
	 * Loops over restricted choice sets to 
	 * determine the highest number of used routes
	 * in restricted choice sets. 
	 *  
	 * @return the size of the largest choice set
	 */
	public int maxChoiceSetSize() {
		int maxChoiceSetSize = 0;
		for (HashMap<Integer,OD> m: ods.values()) {
			for (OD od: m.values()) {
				int routesInThisOD = 0;
				for (Path path: od.restrictedChoiceSet) {
					if (path.getFlow() > 0) {
						routesInThisOD ++;
					}
				}
				maxChoiceSetSize = Math.max(maxChoiceSetSize, routesInThisOD);
			}
		}
		return maxChoiceSetSize;
	}

	/**
	 * Loops over restricted choice sets to 
	 * determine the smallest number of used routes
	 * in restricted choice sets. 
	 *  
	 * @return the size of the smallest choice set
	 */
	public int minChoiceSetSize() {
		double minChoiceSetSize = Integer.MAX_VALUE;
		for (HashMap<Integer,OD> m: ods.values()) {
			for (OD od: m.values()) {
				int routesInThisOD = 0;
				for (Path path: od.restrictedChoiceSet) {
					if (path.getFlow() > 0) {
						routesInThisOD ++;
					}

				}
				minChoiceSetSize = Math.min(minChoiceSetSize, routesInThisOD);
			}
		}
		return minChoiceSetSize();
	}

	/**
	 * The recursive algorithm that enumerates the 
	 * universal choice set; its basic principle is that
	 * the universal choice set from A to B equals the 
	 * union of the universal choice sets from the neighbours of
	 * A to B. To eliminate acyclic routes, recursive calls do not 
	 * include already visited nodes. Finally, the base case is when
	 * B is a neighbor of A. See the algorithm implementation to 
	 * better understand the concept if needed.
	 * 
	 * @param od the OD the holds the destination to which to
	 * find all shortest path
	 * @param u the ID of the node from which to find all shortest paths
	 * @param currentPath the path which was taken to get to u from the origin
	 * in the OD
	 * @param unvisited an array such that unvisited[i] is true if node i
	 * has not yet been visited
	 */
	private void minos(OD od, int u, int[] currentPath, double lengthOfCurrentPath, boolean[] unvisited, double maximumToleratedPathCostFromOtoD, double relativeSubtourLength) {
		// Recursive function to enumerate and save all acyclic paths.
		for (int v : this.getNode(u).getNeighbours()) {	 //kig igennem alle naboknuder til u
			if (v == od.D) {// Base case: The considered node is the
				// destination. Add path and cont.
				if (lengthOfCurrentPath>maximumToleratedPathCostFromOtoD) { //Tjek om det kører
					continue;												//Tjek om det kører
				}															//Tjek om det kører. burde også lave local tjek
				int[] newNodeSeq = new int[currentPath.length + 1]; //Initier nodesekvens der er antal kanter plus 1 (alle knuder der er besøgt undervejs
				for (int i = 0; i < currentPath.length ; i++) {
					newNodeSeq[i] = Integer.valueOf(currentPath[i]); //indsæt værdier i nodesekvens
				}
				newNodeSeq[newNodeSeq.length - 1] = v; //Indsæt sidste knude, dvs. Destination

				this.addPathToUniversalChoiceSet(od, newNodeSeq, relativeSubtourLength -1); // add path to R
				totalNumberOfPaths++;
				totalNumberOfNodesInPaths += newNodeSeq.length;
			} else if (unvisited[v - 1] && lengthOfCurrentPath + dijkstraDists.get(v).get(od.D) <= maximumToleratedPathCostFromOtoD) { // hvis endnu ikke besøgt og hvis afstand dertil samt korteste afstand til destination< global threshold, så gør... // Else, find all acyclic routes from
				// unvisited neighbours
				boolean localConstraintViolated = false;


				/* Although the path does not violate the global cost criterion, it might violate a local cost criterion.
				   Since this is tested every time the path is expanded, it only needs to be checked for the subtours from any of the existing nodes to the new node.
				   If the criterion is violated for any of the subtours, the path is discontinued. */

				double lengthOfSubtour = edgesNodePair.get(currentPath[currentPath.length-1]).get(v).getGenCost(); //kig på nærmeste knude først
				double shortestSubtourLength = dijkstraDists.get(currentPath[currentPath.length-1]).get(v); //korteste vej
				/*First if is comparing to shortest path to previous node visited. Note that dijkstraDists refers to a hashmap with shortest paths between all node pairs (previously generated)*/
				if(lengthOfSubtour <= shortestSubtourLength * localMaximumCostRatioCutOff){
					/*if not violated, then loop through all previous nodes visited - potentially until origin*/
					for(int i = currentPath.length  - 2; i >= 0; i--){
						lengthOfSubtour += edgesNodePair.get(currentPath[i]).get(currentPath[i+1]).getGenCost();
						shortestSubtourLength = dijkstraDists.get(currentPath[i]).get(v);
						if( lengthOfSubtour >  shortestSubtourLength * localMaximumCostRatioCutOff ){
							/*if local detour constraint violated, then break*/
							localConstraintViolated = true;
							relativeSubtourLength = lengthOfSubtour / shortestSubtourLength;
							break;
						} else if( lengthOfSubtour / shortestSubtourLength > relativeSubtourLength ){
							relativeSubtourLength = lengthOfSubtour / shortestSubtourLength; //hvis ikke violated men større end nuværende detour, så udregn ny detour 
						}
					}
				} else { //altså hvis den første (nabo) violater
					relativeSubtourLength = lengthOfSubtour / shortestSubtourLength; 
					localConstraintViolated = true;
				}
				if(localConstraintViolated){
					continue; // stop branch fordi violated, kig på næste i stak, intet nyt genereret
				}


				double lengthOfNewCurrentPath = lengthOfCurrentPath + edgesNodePair.get(u).get(v).getGenCost(); //opdater cost

				int[] newCurrentPath = new int[currentPath.length + 1]; //generer ny current path med sidste knude så man ikke får overskrevet
				//a deepcopy is required here to avoid paths being modified 
				//		from other recursions
				for (int i = 0; i < currentPath.length; i++) {
					newCurrentPath[i] = currentPath[i]; //fyld new current path
				}
				newCurrentPath[newCurrentPath.length - 1] = v; //tilføj også nuværende besøgte knude

				boolean[] newUnvisited = new boolean[unvisited.length]; //lav også her deep-copy - andre branches må godt besøge samme knude igen, men det må den branch man er ud af ikke.
				for (int i = 0; i < newUnvisited.length; i++) {
					newUnvisited[i] = unvisited[i];
				}
				newUnvisited[v - 1] = false; //den man lige har besøgt er ikke unvisited

				minos(od, v, newCurrentPath, lengthOfNewCurrentPath, newUnvisited, maximumToleratedPathCostFromOtoD,
						relativeSubtourLength); //for knude v (den man lige er kommet til) gør man det samme som man gjorde for u før, i.e. besøg alle naboer til v.
			}
		}
		return;
	}

	//TODO check formatting of code part
	/**
	 * Takes in a sequence of integers corresponding to nodes
	 * and returns a list of edges corresponding to the
	 * pairs of nodes that occur in the series:
	 * 
	 * <code>
	 * in:  node1|node2|node3
	 * out:  edge12 | edge23
	 * </code>
	 * @param nodeSeq sequence of node IDs on the path 
	 * @return an array list of edges corespondding to the 
	 * passed node sequence
	 */
	private ArrayList<Edge> nodeSeqAsEdgeList(int[] nodeSeq) {
		ArrayList<Edge> edges = new ArrayList<Edge>();
		for (int i = 0; i < nodeSeq.length - 1; i++) {
			edges.add(getEdge(nodeSeq[i], nodeSeq[i+1]));
		}
		return edges;
	}

	/**
	 * Attempts to print to the File file the contents
	 * of the restricted choice sets for each OD.
	 * Output includes for each path:
	 * 		 <ul>  
	 *            <li> Origin node </li>
	 *            <li> Destination node </li>
	 *            <li> Node sequence </li>
	 *            <li> Choice probability from last iteration </li>
	 *            <li> Flow </li>
	 *            <li> Total travel time on route </li>
	 *            </ul>
	 * @param file the file to write
	 * @throws FileNotFoundException throws an exception if the 
	 * file was not found and could not be created.
	 */
	public void printChoiceSets(File file) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(file);
		String delim = ";";
		out.print("O");
		out.print(delim);
		out.print("D");
		out.print(delim);
		out.print("Path");
		out.print(delim);
		out.print("Choice-P");
		out.print(delim);
		out.print("Flow");
		out.print(delim);
		out.print("Generalized-cost");
		out.print(delim);
		out.print("Length");
		out.print(delim);
		out.print("Time");
		out.print(delim);
		out.print("MaxLocalDetour");
		out.println();
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				for (Path path: od.restrictedChoiceSet) {
					if (path.getFlow() >= minimumFlowToBeConsideredUsed) { //for each used path
						//						print the path
						out.print(od.O);
						out.print(delim);
						out.print(od.D);
						out.print(delim);
						for (Edge edge: path.edges) {
							out.print(edge.getTail());
							out.print(" ");
						}
						out.print(path.edges.get(path.edges.size()-1).getHead()); //Print last node in path
						out.print(delim);
						out.print(path.p);
						out.print(delim);
						out.print(path.getFlow());
						out.print(delim);
						out.print(path.genCost);
						out.print(delim);
						out.print(path.length);
						out.print(delim);
						if (RUM.betaTime > 0) {
						//	out.print( (path.genCost-getDetourednessParameter()*path.getMaximumRelativeSubtourLength()-RUM.betaLength * path.length) / RUM.betaTime ); } else {out.print(-1);
							out.print((path.genCost-RUM.betaLength*path.length)/RUM.betaTime);} else {out.print(-1);
						
						}
						out.print(delim);
						if (localMaximumCostRatio>=1) {
							path.updateMaximumRelativeSubtourLength();
							out.print(path.getMaximumRelativeSubtourLength());
						} else {
							out.println(-1);
						}
						out.println();
					}

				}
			}
		}
		out.close();
	}

	/**
	 * Print aggregated statistics about the restricted choice sets to the file
	 * {@code file} in the .csv format using the default delimiter ";".
	 * 
	 * @see Network#printChoiceSetSummary(File, String)
	 * @param file the file to print to 
	 * @throws FileNotFoundException throws an exception if the file 
	 * coud not be written
	 */
	public void printChoiceSetSummary(File file) throws FileNotFoundException {
		String defaultDelimiter = ";";
		printChoiceSetSummary(file, defaultDelimiter);
	}

	/**
	 * Print aggregated statistics about the restricted choice sets to the file
	 * {@code file} in the .csv format using a specified delimiter 
	 * 
	 * @param file the file to attempt to write to
	 * @param delim the delimiter for the .csv output
	 * @throws FileNotFoundException throws an exception if the file 
	 * could not be written
	 */
	private void printChoiceSetSummary(File file,String delim) throws FileNotFoundException {
		double avgSetSize = calculateAvgChoiceSetSize();
		double maxSetSize = maxChoiceSetSize();
		PrintWriter out = new PrintWriter(file);
		out.print("Average-choice-set-size");
		out.print(delim);
		out.println(avgSetSize);

		out.print("Max-choice-set-size");
		out.print(delim);
		out.println(maxSetSize);

		out.close();
	}

	/**
	 * Attempts to print to the file {@code file} a list of 
	 * link flows by edge ID. Also prints the link time. Uses
	 * the default delimiter ", ".
	 * @param file the file to write
	 * @throws FileNotFoundException throws an exception if the 
	 * file could not be written
	 */
	public void printFlowSolution(File file) throws FileNotFoundException {
		String defaultDelim = ";";
		printFlowSolution(file, defaultDelim);
	}

	/**
	 * Attempts to print to the file {@code file} a list of 
	 * link flows by edge ID. Also prints the link time. Uses
	 * the specified delimiter {@code delim}
	 * 
	 * @param delim the delimiter to use in the .csv file
	 * @param file the .csv file to write (will be created if it does not exist)
	 * @throws FileNotFoundException throws an exception if the file could not 
	 * be found and could not be created
	 */
	public void printFlowSolution(File file,String delim) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(file);
		out.print("EdgeID");
		out.print(delim);
		out.print("Flow");
		out.print(delim);
		out.println("Time");
		//out.print("\n");
		for (Edge edge: edges.values()) {
			out.print(edge.getId());
			out.print(delim);
			out.print(edge.getFlow());
			out.print(delim);
			out.println(edge.getTime());
			//out.print("\n");
		}
		out.close();
		System.out.println("Flow solution was sucessfully output to " + file.getName() + ".");
	}


	/**
	 * Print the flow solution in the same way as 
	 * {@linkplain Network#printFlowSolution(File)}, but without
	 * specifying the file name. Instead creates a file called 
	 * "flow_" + {@code timestamp}, where {@code timestamp}
	 * has the format "yyy_MM-dd_HH_mm_ss".
	 * 
	 * @param folderName the folder to create a timestamped 
	 * output folder in 
	 */
	public void printFlowSolution(String folderName) {
		if (!folderName.endsWith("/")) folderName = folderName + "/";
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		Date date = new Date();
		String fileName = "flow_" + dateFormat.format(date) + ".csv"; 

		File file = new File(folderName + "/" + fileName);
		try {
			printFlowSolution(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The preferred way to create output diagnostics. Calls {@code printOutput} 
	 * with a time stamp as the folder name, and outputs to the folder:
	 * <ul>
	 * 	<li> Detailed information about all restricted choice sets (see {@linkplain Network#printChoiceSets(File)}) </li>
	 * 	<li> Aggregate information about restricted choice set composition: 
	 * min, avg and max size (see {@linkplain Network#printChoiceSetSummary(File)} </li>
	 * 	<li> Convergence pattern passed to this method (see {@link ConvergencePattern#printToFile(File)} </li>
	 * 	<li> Flow solution (see {@linkplain Network#printFlowSolution(File)} </li>
	 * 	<li> Parameters of the route choice model solution algorithm and RUM parameters  </li>
	 * </ul>
	 * 
	 * @param folderName The name of an existing folder in which to create a new folder for the output.
	 * @param rcm the instance of the {@link RouteChoiceModel} class whose parameters to print.
	 * @param conv An instance of the {@link ConvergencePattern} to print.
	 */

	public void printOutput(String folderName,RSUET rcm, ConvergencePattern conv) {
		printOutput(folderName, rcm, conv, null);
	}

	/**
	 * @param folderName The name of an existing folder in which to create a new folder for the output.
	 * @param rcm the instance of the {@link RouteChoiceModel} class whose parameters to print.
	 * @param conv An instance of the {@link ConvergencePattern} to print.
	 * @param nameOfOutputFolder Name of the output folder which is created.
	 */
	public void printOutput(String folderName,RSUET rcm, ConvergencePattern conv,String nameOfOutputFolder) {
		String outFolderName;
		//make sure that folder name ends with a "/"
		if (!folderName.endsWith("/")) folderName = folderName + "/";
		if (nameOfOutputFolder == null) {
			//Define a date format for the time stamp 
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
			Date date = new Date();
			String timestamp = dateFormat.format(date);

			//This method creates a folder "outFolderName" and puts relevant information in it
			outFolderName = folderName + getNetworkName() + "-Output_" + timestamp + "/";
		} else {
			outFolderName = folderName + nameOfOutputFolder;
			if (!outFolderName.endsWith("/")) outFolderName = outFolderName + "/";
		}


		//Output link flow solution
		new File(outFolderName).mkdirs(); //make folder if not exists

		System.out.println("Output Folder is " + outFolderName);

		File file = new File(outFolderName + "flow.csv");
		try {
			printFlowSolution(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		//Output parameters
		rcm.printParamToFile(outFolderName + "parameters.csv");

		//Detailed choice sets
		file = new File(outFolderName + "choice-sets.csv");
		if(printCS) {
			
		
		try {
			printChoiceSets(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		}
		System.out.println("Choice sets were sucessfully output to " + file.getName() + ".");

		file = new File(outFolderName + "choice-set-summary.csv");
		try {
			printChoiceSetSummary(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("Choice set summary was sucessfully output to " + file.getName());

		file = new File(outFolderName + "convergence.csv");
		try {
			conv.printToFile(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("Convergence pattern was output to " + file.getName());
	}

	/**
	 * Read method which can read network triplets as they are formatted in https://github.com/bstabler/TransportationNetworks.
	 * 
	 * @param filename1 [network]_net.tntp
	 * @param filename2 [network]_node.tntp
	 * @param filename3 [network]_trips.tntp
	 * @see Network#Network(String)
	 */
	private void readNetworkRaw(String filename1, String filename2, String filename3) { 
		System.out.println("Reading network... this may take a while.");
		//Assumes  that  the  .txt  file  has  the  structure  of  the  Sioux  Falls  data
		try {

			System.out.print("Reading edges... ");
			// Read edge data
			
			File file = new File(filename1);
			Scanner rowScanner = new Scanner(file);

			//rowScanner.useDelimiter("	");
			rowScanner.useLocale(Locale.ENGLISH);

			//Read the metadata;

			int numNodes = -1;
			int numEdges = -1;
			boolean keepReadingMetadata = true;
			int maxMetaDataLines = 100;
			int metaDataLines = 0;
			while (keepReadingMetadata == true) {
				//Read header line as string
				String headerLine = rowScanner.nextLine();
				headerLine = headerLine.toUpperCase();
				String checkString = "<END OF METADATA>";
				if (headerLine.startsWith(checkString)) {
					break;
				}
				//				Check for relevant data
				String searchString = "<NUMBER OF NODES>";
				if (headerLine.startsWith(searchString)) {
					Scanner headerLineScanner = new Scanner(headerLine);
					headerLineScanner.skip(searchString);
					numNodes = headerLineScanner.nextInt();
					headerLineScanner.close();
				}
				searchString = "<NUMBER OF LINKS>";
				if (headerLine.contains(searchString)) {
					Scanner nodeLineScanner = new Scanner(headerLine);
					nodeLineScanner.useLocale(Locale.ENGLISH);
					nodeLineScanner.skip(searchString);
					numEdges = nodeLineScanner.nextInt();
					nodeLineScanner.close();
				}
				metaDataLines ++;
				if (metaDataLines > maxMetaDataLines) {
					keepReadingMetadata = false;
				}

			}

			edges = new HashMap<Integer,Edge>();
			int edgeIndex = 0;
			//			find the first occurence of "~"
			boolean foundHeaderToken = false;
			while (rowScanner.hasNextLine()) {
				String dataLine = rowScanner.nextLine();
				if (dataLine.contains("~")) {
					//					This was the header. Proceed.
					foundHeaderToken = true;
					break;
				}
			}
			if (!foundHeaderToken) {
				System.err.println("No edges read did not encounter header token ~");
			}

			while (rowScanner.hasNextLine()) {
				String dataLine = rowScanner.nextLine();
				if (!dataLine.equals("")){
					Scanner in = new Scanner(dataLine);
					in.useLocale(Locale.ENGLISH);
					Edge thisEdge = new Edge();
					thisEdge.setId(edgeIndex + 1);
					thisEdge.setTail(in.nextInt());
					thisEdge.setHead(in.nextInt());
					thisEdge.setCapacity(in.nextDouble());
					thisEdge.setLength(in.nextDouble());
					thisEdge.setFreeFlowTime(in.nextDouble());
					thisEdge.setB(in.nextDouble());
					thisEdge.setPower(in.nextDouble());
					edges.put(thisEdge.getId(),thisEdge);
					in.close();
					edgeIndex++;
				}

			}
			rowScanner.close();
			System.out.println("Done!");

			System.out.print("Reading nodes... ");
			// Read node data

			file = new File(filename2);
			if (!file.exists()) {
				file.createNewFile();
			}
			rowScanner = new Scanner(file);

			nodes = new HashMap<Integer,Node>();
			boolean didRead = false;
			while (rowScanner.hasNextLine()) {
				String dataLine = rowScanner.nextLine();
				dataLine = dataLine.toLowerCase();
				if (!dataLine.equals("") && !dataLine.startsWith("node")) {
					didRead = true;
					Scanner in = new Scanner(dataLine);
					in.useLocale(Locale.ENGLISH);
					Node node = new Node();
					int nodeId = in.nextInt();
					node.setId(nodeId);
					node.setX(in.nextDouble());
					node.setY(in.nextDouble());
					nodes.put(nodeId, node);
					in.close();
				}

			}
			//			it is permitted to have an empty node file or none at all.
			//			In this case, nodes are implicit and enumerated ascending 
			//			from 1, with the coordinates 0,0.
			rowScanner.close();
			if (!didRead) {
				for (int i = 0; i < numNodes; i++) {
					Node node = new Node();
					int Id = i + 1;
					node.setId(Id);
					node.setX(0);
					node.setY(0);
					nodes.put(Id, node);
				}
			}

			System.out.println("Done!");
			System.out.println("Reading demand: ");
			System.out.println("ODs read   Time [s]   Free memory [mb]");

			// Read OD data (trip demand)
			file = new File(filename3);
			rowScanner = new Scanner(file);
			rowScanner.useLocale(Locale.ENGLISH);
			rowScanner.useDelimiter("	");

			// Read metadata


			ods = new HashMap<Integer,HashMap<Integer,OD>>();
			int originNode = 0;
			int destinationNode = 0;
			double demand = -1;
			boolean proceedToMainData = false;
			while (!proceedToMainData) {
				String checkString = rowScanner.nextLine();
				if (checkString.contains("<END OF METADATA>")) {
					proceedToMainData = true;
				}
			}
			//Here rowScanner should be at Origin 1
			int numOD = 0;
			int modCounter = 0;
			long beginTime = System.currentTimeMillis();
			while (rowScanner.hasNextLine()) {
				String nextLineAsString = rowScanner.nextLine();
				nextLineAsString = nextLineAsString.toLowerCase();
				Scanner in = new Scanner(nextLineAsString);
				in.useLocale(Locale.ENGLISH);
				if (nextLineAsString.startsWith("origin")) {
					in.next(); //skip "Origin"
					originNode = in.nextInt();
				} else if (nextLineAsString != "") {
					in.useDelimiter(";\\s*");
					while (in.hasNext()){
						String nextOdAsString = in.next();
						Scanner odScanner = new Scanner(nextOdAsString);
						odScanner.useLocale(Locale.ENGLISH);
						destinationNode = odScanner.nextInt();
						odScanner.next(); //skip :
						demand = odScanner.nextDouble() * demandScale;
						if (demand > 0) {
							if (!ods.containsKey(originNode)) {
								ods.put(originNode, new HashMap<Integer,OD>());
								numOD ++;
								if(numOD % Math.pow(2,modCounter) == 0 ){
									modCounter++;
									System.out.printf("%-8s   ",numOD);
									System.out.printf("%-8s   ", (System.currentTimeMillis() - beginTime) / 1000);
									System.out.printf("%-11s\n", Runtime.getRuntime().freeMemory() / 1048576);
								}
							}
							ods.get(originNode).put(destinationNode, new OD(originNode, destinationNode, demand));
						}
						odScanner.close();
					}
				}
				in.close();
			}
			rowScanner.close();
			this.numOD = numOD;
			System.out.println("Done!");

			// Generate conditional data
			// Generate neighbours

			System.out.print("Finalising... ");
			for (Edge edge: edges.values()) {
				int tail = edge.getTail();
				int head = edge.getHead();
				// For each edge, the head is a neighbour to the tail.
				this.getNode(tail).getNeighbours().add(head);
				//Map node pairs to edges
				if (!edgesNodePair.containsKey(tail)) {
					edgesNodePair.put(tail, new HashMap<Integer,Edge>());
				}
				edgesNodePair.get(tail).put(head, edge);

				if(isNetworkBidirectional){
					Edge oppositeEdge = createOppositeEdge(edge);
					if (!edgesNodePair.containsKey(oppositeEdge.getTail())) {
						edgesNodePair.put(head, new HashMap<Integer,Edge>());
					}
					edgesNodePair.get(oppositeEdge.getTail()).put(oppositeEdge.getHead(), oppositeEdge);
				}
			}

			//Register which nodes have demand
			for (HashMap<Integer, OD> m: ods.values()) {
				for (OD od: m.values()) { // For each OD-pair
					this.getNode(od.O).setHasDemandFrom(true);
					this.getNode(od.D).setHasDemandTo(true);
				}
			}
			System.out.println("Done!");


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Network successfully read.");
	}





	/**
	 * 
	 */

	private Edge createOppositeEdge(Edge edge){
		Edge oppositeEdge = new Edge();
		oppositeEdge.setB(edge.getB());
		oppositeEdge.setCapacity(edge.getCapacity());
		oppositeEdge.setFreeFlowTime(edge.getFreeFlowTime());
		oppositeEdge.setGenCost(edge.getGenCost());
		oppositeEdge.setHead(edge.getTail());
		oppositeEdge.setId(-edge.getId());
		oppositeEdge.setLength(edge.getLength());
		oppositeEdge.setPower(edge.getPower());
		oppositeEdge.setTail(edge.getHead());
		return oppositeEdge;
	}

	/**
	 * Calculates the gap measure on used routes (inner convergence measure)
	 * as defined by their transformed cost; corresponds to 
	 * eq. (27) in Rasmussen et al, 2015, and generalized for the TSUE
	 * via the value returned by {@linkplain Path#computeTransformedCost(RouteChoiceModel)}.
	 * 
	 * @return the relative used gap on the network
	 */
	public double relGapUsed() { // 
		// Corresponds to eq. (27) in Rasmussen et al, 2015, and generalised for the TSUE
		double cmin; // Minimum transformed cost
		double enumerator = 0;
		double denominator = 0;

		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				// Find minimum cost path for that OD
				cmin = od.getMinimumTransformedCost();

				// Add appropriate terms to running sum
				for (Path path : od.restrictedChoiceSet) {
					double flow = path.getFlow();
					if (flow > 0) {
						enumerator += flow * (path.transformedCost - cmin);
						denominator += flow * (path.transformedCost);
					}

				}
			}
		}
		double relGapUsed = enumerator / denominator;
		return relGapUsed;
	}
	
	public double gapLocalDetour() { // 
		
		double valuesum = 0.0;
		int count=0;
		double diff=0;
		
				for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				// Find minimum cost path for that OD
				
				// Add appropriate terms to running sum
				for (Path path : od.restrictedChoiceSet) {
					double flow = path.getFlow();
					if (flow >= 0) {
						if (Math.abs(path.getFlow()- path.getAuxFlow())>diff) 
						{
							diff=Math.abs(path.getFlow()- path.getAuxFlow());
							System.out.println(
									"flow-auxflow_MAX:" + Math.abs(path.getFlow()- path.getAuxFlow())+";"+path.getFlow()+";"+path.getAuxFlow());
						}
						valuesum += Math.pow(Math.abs(path.getFlow()- path.getAuxFlow()), 2);
						count+=1;
											}
					
			}
		}
				}
		double relGapUsed = Math.sqrt(valuesum/count);
		return relGapUsed;
	}

	/**
	 * Set all network edge flows to 0, and
	 * restricted choice sets to 0.
	 */
	public void resetNetwork() {
		for (Edge edge : this.edges.values()) {
			edge.setFlow(0);
		}

		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				od.restrictedChoiceSet = new ArrayList<Path>();
				//			od.updateDelta();
			}
		}
	}

	/**
	 * /**
	 * Important and common problem of assigning probabilities and 
	 * auxiliary flows using the RUM when assuming given LoS as defined by 
	 * state of the {@code network} object. Also updates flows on paths
	 * to oldFlow * (1-gamma) + auxiliaryFlow * (gamma).
	 * 
	 * @param rum the random utility model to determine the choice probabilities.
	 * @param gamma the relative trust in the auxiliary solution; gamma is between
	 * 0 and 1.
	 */
	public void restrictedInnerMasterProblem(RUM rum, double gamma) {
		// Calculate and save pathwise-auxiliary flows
		double denominatorInProbabilityExpression;
		double denominatorInDetourExpression;
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				// Calculate utility logsum for MNL
				denominatorInProbabilityExpression = 0;
				denominatorInDetourExpression = 0;
				for (Path path : od.restrictedChoiceSet) {
					path.enumeratorInProbabilityExpression = rum.computeEnumeratorInProbabilityExpression(path);
					denominatorInProbabilityExpression += path.enumeratorInProbabilityExpression;
					
					if (detourednessParameter > 0 ) {
						//path.updateMaximumRelativeSubtourLength();
						path.enumeratorInDetourExpression = Math.max(Math.exp(-1*rum.thetaLocalD*(path.getMaximumRelativeSubtourLength() - detourednessParameter))-1,0);
						denominatorInDetourExpression += path.enumeratorInDetourExpression;
						
						if (denominatorInDetourExpression < 0.1) {
							System.out.println("MaximumRelativeSubtourLength: " + path.getMaximumRelativeSubtourLength());
							
						}
						
						
					}
				}
				
				if (detourednessParameter > 0 ) {
					double probsum;
					probsum=0;
					for (Path path : od.restrictedChoiceSet) {
						probsum += (path.enumeratorInProbabilityExpression / denominatorInProbabilityExpression)*(path.enumeratorInDetourExpression/denominatorInDetourExpression);
					}
					for (Path path : od.restrictedChoiceSet) {
						path.p = ((path.enumeratorInProbabilityExpression / denominatorInProbabilityExpression)*(path.enumeratorInDetourExpression/denominatorInDetourExpression)) / probsum;
					}
				}
				
				if (detourednessParameter <= 0 ) {
					
					for (Path path : od.restrictedChoiceSet) {
						path.p = path.enumeratorInProbabilityExpression / denominatorInProbabilityExpression;
					}
				}
				
				for (Path path: od.restrictedChoiceSet) {
					path.setAuxFlow(od.demand * path.p);
					path.setFlow(path.getFlow() * (1 - gamma) + path.getAuxFlow() * gamma);
				}
			}
		}
	}


	/**
	 * Sets the 
	 * @param localMaximumCostRatioCutOff
	 */
	public void setLocalMaximumCostRatioCutOff(double maximumCostRatioCutOff){
		this.localMaximumCostRatioCutOff = maximumCostRatioCutOff;
	}

	public void setLocalMaximumCostRatio(double maximumCostRatio){
		this.localMaximumCostRatio = maximumCostRatio;
	}
	public double getLocalMaximumCostRatio(){
		return this.localMaximumCostRatio;
	}
	



	/**
	 * Sets the 
	 * @param maximumCostRatio
	 */
	public void setMaximumCostRatio(double maximumCostRatio){
		this.maximumCostRatio = maximumCostRatio;
	}
	public void setMaximumCostDeviation(double maximumCostDeviation){
		this.maximumCostDeviation = maximumCostDeviation;
	}


	/**
	 * Uses the predecessors of each nodes found by a dijktra's algorithm
	 * to determine the shortest path from O to D. This method only
	 * works when dijkstra's algorithm was last called on node {@code od.O}.
	 * 
	 * @param od the OD between which to find the shortest path based on
	 * the last dijkstra search performed on the origin. 
	 * @return the shortest path from {@code od.O} to {@code od.D}.
	 */
	private Path shortestPath(OD od) {
		Node origin = getNode(od.O);
		Node destination = getNode(od.D);

		// First construct inverse sequence as array list
		ArrayList<Integer> invSeq = new ArrayList<Integer>();
		// Backtrack the path from the destination until origin is reached
		Node u = destination;
		while (u != origin) {
			invSeq.add(u.getId());
			u = u.dijkstraPrev;
		}
		// Add origin
		invSeq.add(origin.getId());

		int numEdgesInPath = invSeq.size()-1;
		ArrayList<Edge> edgesInPath = new ArrayList<Edge>(numEdgesInPath);
		//Iterate backwards
		for (int i = numEdgesInPath; i > 0; i--) {
			edgesInPath.add(edgesNodePair.get(invSeq.get(i)).get(invSeq.get(i-1)));
		}

		Path path = new Path(edgesInPath,od);

		return path;
	}

	/**
	 * Sort the universal choice set by their natural ordering.
	 * 
	 * @see Path#compareTo(Path)
	 */
	public void sortUniversalChoiceSets() {
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				Collections.sort(od.R);
			}
		}
	}

	/**
	 * Tests if the total flow assigned on paths in the restricted choice set
	 * for each OD is equal to the demand, with a tolerance of {@code precision}. 
	 * 
	 * @param tolerance the maximum allowed relative deviation from the demand
	 * to consider the assigned flow correct
	 * @return the first found OD where the sum of assigned flow on the restricted
	 * choice set does not equal the demand on the OD.
	 */
	public OD testDemandIntegrity(double tolerance) {
		for (HashMap<Integer,OD> m: ods.values()) {
			for (OD od: m.values()) {
				if( testDemandIntegrity(od, tolerance)) return od;
			}
		}
		return null;
	}

	/**
	 * Tests if the demand on a specific OD
	 * equals the sum of flows on routes in the 
	 * restricted choice set on that OD.
	 * 
	 * @param od the OD to be tested
	 * @param tolerance the maximum allowed relative deviation from the demand
	 * @return true if the demand does not correspond to the assigned flow 
	 * within the tolerance, false otherwise
	 */
	private boolean testDemandIntegrity(OD od, double tolerance) {
		double sum = 0;
		for (Path path: od.restrictedChoiceSet) {
			sum += path.getFlow();
		}
		return (Math.abs(od.demand - sum)/od.demand > tolerance);
	}

	private void throwNetworkReadExceptionFileNotFound(String fileEnding) {
		throw new InputMismatchException("Did not find "+ fileEnding + " file.");
	}

	/**the unrestricted master problem, as opposed to the restricted one, 
	 * does not assume that all routes in {@code restrictedChoiceSet} are
	 * used. Therefore it also requires a reference cost function to 
	 * specify the threshold which defines if a route is used or not.
	 * 
	 * @param rum the random utility model to be used to determine choice probabilities
	 * on used routes
	 * @param omega the threshold reference cost function
	 * @param gamma the relative trust in the auxiliary solution
	 */
	public void unrestrictedMasterProblemInnerLogit(RUM rum, RefCostFun omega, double gamma) {
		// Calculate and save pathwise-auxiliary flows
		double denominatorInProbabilityExpression;
		double denominatorInDetourExpression;
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) {
				// For each OD-pair
				// Calculate utility logsum for MNL
				denominatorInProbabilityExpression = 0;
				denominatorInDetourExpression = 0;
				double threshold = omega.calculateRefCost(od);
				for (Path path : od.restrictedChoiceSet) {
					
					if (path.genCost <= threshold) {
						path.enumeratorInProbabilityExpression = rum.computeEnumeratorInProbabilityExpression(path);
						denominatorInProbabilityExpression += path.enumeratorInProbabilityExpression;
						path.setHasBeenUsed(true);
//						System.out.println("denominatorInProbabilityExpression: " + denominatorInProbabilityExpression);
						
					} else path.enumeratorInProbabilityExpression = 0;

				}
				
				
				if (detourednessParameter > 0 ) { //!!!!!!!!!!!!
					//path.updateMaximumRelativeSubtourLength();
					int k=0;
					for (Path path : od.restrictedChoiceSet) {
						path.updateMaximumRelativeSubtourLength();
						k+=1;
						//System.out.print("pathID"+k + "\n");
						//System.out.print("maximumRelativeSUbTourLength"+path.getMaximumRelativeSubtourLength() + "\n");
						//System.out.print("pathCost:"+path.genCost + "\n");
						//System.out.print("threshold:"+threshold + "\n");
						if (path.getMaximumRelativeSubtourLength() <= detourednessParameter) {
								path.enumeratorInDetourExpression = Math.max(Math.exp(-1*rum.thetaLocalD*(path.getMaximumRelativeSubtourLength() - detourednessParameter))-1,0);
					
								denominatorInDetourExpression += path.enumeratorInDetourExpression;
								//System.out.print("denominatorDetour"+denominatorInDetourExpression + "\n");
							} 
						else {path.enumeratorInDetourExpression=0;
						denominatorInDetourExpression += path.enumeratorInDetourExpression;
							}
						}
					}
				if (detourednessParameter > 0 ) { 
					double probsum;
					probsum=0;
					for (Path path : od.restrictedChoiceSet) {
						probsum += (path.enumeratorInProbabilityExpression / denominatorInProbabilityExpression)*(path.enumeratorInDetourExpression/denominatorInDetourExpression);
					}
					for (Path path : od.restrictedChoiceSet) {
						path.p = ((path.enumeratorInProbabilityExpression / denominatorInProbabilityExpression)*(path.enumeratorInDetourExpression/denominatorInDetourExpression)) / probsum;
						//System.out.print(path.p+";"+path.getMaximumRelativeSubtourLength()+";"+path.enumeratorInDetourExpression+";"+denominatorInDetourExpression+";"+probsum+"\n");
						
					}
				}
				
				if (detourednessParameter <= 0 ) {
					
					for (Path path : od.restrictedChoiceSet) {
						path.p = path.enumeratorInProbabilityExpression / denominatorInProbabilityExpression;
						
					}
				}
				
				for (Path path: od.restrictedChoiceSet) {
					path.setAuxFlow(od.demand * path.p);
					double flowAfter = path.getFlow() * (1 - gamma) + path.getAuxFlow() * gamma;
					
					if (Double.isNaN(flowAfter)) {
					System.out.println("OD: "+ od.O +"-"+od.D + "flow after: " + flowAfter + "; getFlow: " + path.getFlow() + "; getAuxFlow: " + path.getAuxFlow() + "; p: " + path.p + "; enumeratorProb: " + path.enumeratorInProbabilityExpression + "; enumeratordeto: " + path.enumeratorInDetourExpression
							+ " denomProb: "+ denominatorInProbabilityExpression+ " denomdetour: "+ denominatorInDetourExpression+" maxDetour: "+ path.getMaximumRelativeSubtourLength());
					}
					path.setFlow(flowAfter);
				}
			}
		}
	}

	/**
	 * Updates the cost on edges to correspond to the current flows on them
	 * @param rum the RUM from which to extract the generalized cost 
	 * (additive deterministic utility)
	 */
	public void updateEdgeCosts(RUM rum) {
		for (Edge edge: edges.values()) {
			edge.updateCost(rum);
		}
	}

	/**
	 * Updates the generalized cost on paths to correspond to the
	 * sum of generalized costs of its edges. Also updates the 
	 * minimum cost on each OD.
	 */

	public void updatePathCosts() {
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				double minCost = this.M;
				for (Path path: od.restrictedChoiceSet) {
					double thisCost = path.updateCost(detourednessParameter);
					if (thisCost < minCost) {
						minCost = thisCost;
					}

				}
				od.setMinimumCost(minCost);
			}
		}

	}

	/**
	 * Updates the Path Size Factors of all paths
	 * in restricted choice sets. Uses generalized 
	 * costs to determine degree of similarity, which
	 * means that path size factors must be updated
	 * every time LoS in the network is updated.
	 * 
	 * @param rum the Random Utility Model from which 
	 * to retrieve the Path Size factor 
	 * @see PSL
	 */
	public void updatePathSizeFactors(RUM rum) {
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				od.updatePathSizeFactors(rum);
			}
		}
	}

	/**
	 * To save computation time of the relatively expensive
	 * operation of re-updating path size factors after
	 * {@link Network#columnGeneration()} in the RSUET(min,omega),
	 *  this method only selectively them where a new path was introduced.
	 * 
	 * @see RSUET#solve(Network)
	 * @param rum the Random Utility Model from which 
	 * to retrieve the Path Size factor 
	 */
	public void updatePathSizeFactorsWherePathsWereAdded(RUM rum) {
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				if (od.pathWasAddedDuringColumnGeneration) {
					od.updatePathSizeFactors(rum);
				}
			}
		}

	}

	/**
	 * Update the transformed costs on all paths in
	 * restricted choice sets, using the passed 
	 * route choice model.
	 * 
	 * @param rcm the route choice model from which 
	 * to retrieve the transformed cost
	 */
	public void updateTransformedCosts(RouteChoiceModel rcm) {
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				double minTransformedCost = Double.POSITIVE_INFINITY;
				for (Path path: od.restrictedChoiceSet) {
					double transCost =  path.updateTransformedCost(rcm);
					if (path.getFlow() > 0 && transCost < minTransformedCost) minTransformedCost = transCost;
				}
				od.setMinimumTransformedCost(minTransformedCost);
			}
		}
	}

	/**
	 * Updates generalized costs of the universal
	 * choice set for each OD to correspond to the
	 * sum of generalized costs of the edges that
	 * constitute them.
	 */
	public void updateUniversalChoiceSetCosts() {
		for (HashMap<Integer, OD> m: ods.values()) {
			for (OD od: m.values()) { // For each OD-pair
				if(useLocalStorage){
					try {
						loadUniversalChoiceSetFromStorage(od);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				for (Path path : od.R) {
					path.updateCost(detourednessParameter); // costs
				}
				if(useLocalStorage){
					try {
						transferUniversalChoiceSetToStorage(od);
					} catch (IOException e) {
						e.printStackTrace();
					}
					od.R.clear();
				}
			}
		}
	}

	/**
	 * Writes the universal choice set;
	 * requires it to be generated. 
	 * 
	 * @see Network#generateInitialRestrictedChoiceSets()
	 * @param filename the name of the file to
	 * output to, as a string
	 */
	public void writeUniversalChoiceSet(String filename) {
		try {
			PrintWriter out = new PrintWriter(filename);
			for (HashMap<Integer, OD> m: ods.values()) {
				for (OD od: m.values()) { // For each OD-pair
					out.println(od.R.size() + " ");
					for (Path path : od.R) {
						int setSize = path.edges.size();
						out.print(setSize + " ");
						for (int i = 0; i < setSize; i++) {
							out.print(path.edges.get(i).getTail() + " ");
						}
						out.print(path.edges.get((setSize - 1))+ " ");
						out.println();
					}
				}
			}
			out.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
	public void updateMaximumRelativeSubtourLength() {
		double globalMaximumValidLocalRelativeDetourLength = 0;
		if(localMaximumCostRatio >= 1) {
			for (HashMap<Integer, OD> m: ods.values()) {
				for (OD od: m.values()) { // For each OD-pair
					double minCost = this.M;
					for (Path path: od.restrictedChoiceSet) {
						path.updateMaximumRelativeSubtourLength();
						if(path.getMaximumRelativeSubtourLength() > globalMaximumValidLocalRelativeDetourLength 
								&& path.getMaximumRelativeSubtourLength() < localMaximumCostRatio ) {
							globalMaximumValidLocalRelativeDetourLength = path.getMaximumRelativeSubtourLength();
						}
					}
					od.setMinimumCost(minCost);
				}
			}
		}
		System.out.println("globalMaximumValidLocalRelativeDetourLength: "+globalMaximumValidLocalRelativeDetourLength);
	}
}
