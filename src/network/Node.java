package network;

import java.util.ArrayList;

import auxiliary.MinPriorityQueue;
import auxiliary.MyMinPriorityQueue;

/**
 * Symbolises a node in a {@link network.Network}. Is identified with an integer id which is read 
 * from the node network file by {@link Network#Network(String)}. 
 * Note: this class has a natural ordering that is inconsistent with equals.
 * 
 * @author mesch
 */
public class Node implements Comparable<Node>{
    	/**
    	 * identification, used by {@link Network#getNode(int)}
    	 */
	private int id;

	/**
	 * first  coordinate for plotting, with unit of measurement
	 * as specified in the network file
	 */
	private double x; 
	/**
	 * second  coordinate for plotting, with unit of measurement
	 * as specified in the network file
	 */
	private double y;
	
	
	private boolean hasDemandFrom = false;
	private boolean hasDemandTo = false;
	
	/**
	 * The shortest path distance to this node 
	 * from the origin where dijkstra's algorithm
	 * was last run. Together with {@link Node#dijkstraPrev} holds all 
         * information to deduce the shortest path from some origin to 
         * any destination with demand to it.
	 */
	public double dijkstraDist = -1;
	
	/**
	 * the predecessor of this node when the origin is 
	 * implicitly given by the last node that dijkstra's 
	 * algorithm was run on.
	 */
	public Node dijkstraPrev = null; 
	
	/**
	 * boolean used in {@linkplain Network#dijkstraMinPriorityQueue(Node)}.
	 * True when the node has been processed to its final state. 
	 */
	public boolean dijkstraVisitied = false;
	
	public boolean hasDemandFrom() {
		return hasDemandFrom;
	}
	public void setHasDemandFrom(boolean hasDemand) {
		this.hasDemandFrom = hasDemand;
	}
	
	/**
	 * ArrayList of IDs of nodes that are connected directly by an outgoing link from this node
	 */
	private ArrayList<Integer> neighbours = new ArrayList<Integer>();
	
	/**
	 * @return the X coordinate of the node
	 */
	double getX() {
		return x;
	}
	void setX(double d) {
		this.x = d;
	}
	/**
	 * @return the Y coordinate of the node
	 */
	double getY() {
		return y;
	}
	void setY(double d) {
		this.y = d;
	}
	/**
	 * @return a saved ArrayList of this node's neighbors
	 */
	ArrayList<Integer> getNeighbours() {
		return neighbours;
	}
	void setNeighbours(ArrayList<Integer> neighbours) {
		this.neighbours = neighbours;
	}
	int getId() {
		return id;
	}
	void setId(int id) {
		this.id = id;
	}
	/**
	 * @return value of the boolean field hasDemandTo
	 */
	public boolean hasDemandTo() {
		return hasDemandTo;
	}
	/**
	 * @param hasDemandTo the hasDemandTo to set
	 */
	public void setHasDemandTo(boolean hasDemandTo) {
		this.hasDemandTo = hasDemandTo;
	}
	/**
	 * Nodes are naturally ordered by {@code dijkstraDist} to 
	 * enable use of a minimum-priority queue.
	 * 
	 * @see MyMinPriorityQueue
	 * @see MinPriorityQueue
	 */
	@Override
	public int compareTo(Node o) {
		return Double.compare(this.dijkstraDist, o.dijkstraDist);
	}
}
