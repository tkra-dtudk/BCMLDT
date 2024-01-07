package network;

import choiceModel.RUM;

/**
 * Symbolizes a link in a {@link network.Network}. 
 * 
 * @author mesch
 * @see network.Network
 * @see network.Node
 */
public class Edge {
    	/**
    	 * id of the edge, read from the node text file
    	 * 
    	 * @see Network#Network(String)
    	 */
	private int id;
	
	/**
	 * tail id of node corresponding to terminal of this edge
	 */
	private int tail; // Node id of tail
	
	/**
	 * head id of node corresponding to origin of this edge
	 */
	private int head; 
	
	/**
	 * capacity practical capacity of link as used in BPR formula {@code time = freeFlowTime * (1 + b*(flow/capacity)^power}
	 */
	private double capacity;
	
	/**
	 * length of this edge
	 */
	private double length; 
	
	/**
	 * freeFlowTime of this edge
	 */
	private double freeFlowTime;
	
	/**
	 * b coefficient as used in BPR formula {@code time = freeFlowTime * (1 + b*(flow/capacity)^power}
	 */
	private double b;
	
	/**
	 * power coefficient as used in BPR formula {@code time = freeFlowTime * (1 + b*(flow/capacity)^power}
	 */
	private double power; 

	/**
	 * flow amount of traffic. Initialized as 0.
	 */
	private double flow = 0;
	
	/**
	 *  auxiliary flow for use in {@link Network#restrictedInnerMasterProblem(RUM, double)}. Initialized as 0.
	 */
	private double auxFlow = 0;
	
	/**
	 * actual travel time on this edge
	 */
	private double time = freeFlowTime;
	
	/**
	 * generalized cost defined by {@link Edge#genCost}
	 */
	private double genCost;

	/**
	 * the number of paths defined in some choice set that contain this edge. 
	 * Cannot be computed internally and is initialized as 0. 
	 * @see OD#updatePathSizeFactors(RUM)
	 */
	private int numPathsWithEdge =0; 

	/**
	 * add flow to current flow
	 * @param flow the flow to be added
	 */
	public void addFlow(double flow) {
		this.flow += flow;
	}
	/**
	 * @return double the time on the link according to BPR formula
	 * according to the BPR formula using the current flow
	 */
	private double bpr(){
		return getFreeFlowTime()*(1+getB()*Math.pow(getFlow()/getCapacity(), getPower()));
	}

	double getAuxFlow() {
		return auxFlow;
	}

	double getB() {
		return b;
	}
	double getCapacity() {
		return capacity;
	}
	double getFlow() {
		return flow;
	}
	double getFreeFlowTime() {
		return freeFlowTime;
	}
	public double getGenCost() {
		return genCost;
	}
	int getHead() {
		return head;
	}
	int getId() {
		return id;
	}
	public double getLength() {
		return length;
	}
	int getNumPathsWithEdge() {
		return numPathsWithEdge;
	}
	double getPower() {
		return power;
	}
	int getTail() {
		return tail;
	}
	public double getTime() {
		return time;
	}
	void setAuxFlow(double auxFlow) {
		this.auxFlow = auxFlow;
	}
	void setB(double b) {
		this.b = b;
	}
	void setCapacity(double capacity) {
		this.capacity = capacity;
	}
	public void setFlow(double flow){
		this.flow = flow;
	}
	void setFreeFlowTime(double freeFlowTime) {
		this.freeFlowTime = freeFlowTime;
	}
	public void setGenCost(double genCost) {
		this.genCost = genCost;
	}
	void setHead(int head) {
		this.head = head;
	}
	void setId(int id) {
		this.id = id;
	}
	void setLength(double length) {
		this.length = length;
	}
	void setNumPathsWithEdge(int delta) {
		this.numPathsWithEdge = delta;
	}
	void setPower(double power) {
		this.power = power;
	}
	void setTail(int tail) {
		this.tail = tail;
	}
	
	/**
	 * Prints a more useful expression when calling
	 * e.g. System.out.print(Edge edge):
	 * <ul>
	 * <li> tail </li>
	 * <li> head </li>
	 * <li> flow </li>
	 * <li> current travel time </li>
	 * </ul>
	 */
	@Override
	public String toString() {
		return "Edge ("+getTail()+","+getHead()+"). Flow: "+getFlow()+". Travel time: "+getTime();
	}
	
	/**
	 * Updates the total travel time on this edge
	 * using its current flow and the BPR formula
	 * with coefficients as specified in the 
	 * [network]_net.tntp file, and then updates
	 * the generalized cost of this
	 *  edge using the specified RUM.
	 *  
	 *  @param rum the RUM to define the generalized
	 *  cost to assign
	 *  @see Edge#updateTime()
	 */
	public void updateCost(RUM rum) {
		updateTime();
		double costToReturn = rum.genCost(this);
		setGenCost(costToReturn);

	}
	
	/**
	 * Updates the total travel time on the edge
	 * to correspond to the BPR formula evaluated
	 * with the current flow, using the BPR formula
	 * parameters {@linkplain Edge#b} and {@linkplain Edge#power}
	 * that were read into the edge at from the 
	 * [network]_net.tntp file at instantiation.
	 */
	private void updateTime() {
		this.time = bpr();
	}


}
