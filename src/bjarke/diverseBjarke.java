package bjarke;

import java.util.HashMap;
import java.util.HashSet;

import auxiliary.MyMinPriorityQueue;
import network.Node;

public class diverseBjarke {
//
//	
//	/*Initially find min distance between all node pairs*/
//	public void generateAllShortestPathTrees(){
//		for (Node origin : nodes.values()){
//			dijkstraMinPriorityQueueWithStorage(origin);
//		}
//	}
//	
//	/* back-search through tree for each Origin, makes data structure (inserts) in dijkstraPrevs and dijkstraDists*/
//	public void dijkstraMinPriorityQueueWithStorage(Node originNode) {
//		dijkstraMinPriorityQueue(originNode);
//		int id = originNode.getId();
//		dijkstraPrevs.put(id,new HashMap<Integer,Integer>());
//		dijkstraDists.put(id,new HashMap<Integer,Double>());
//		for(Node v : nodes.values()){
//			dijkstraPrevs.get(id).put(v.getId(), v.dijkstraPrev.getId());
//			dijkstraDists.get(id).put(v.getId(), v.dijkstraDist);
//		}
//	}
//			/*Dijkstra shortest path tree*/
//	public boolean dijkstraMinPriorityQueue(Node originNode) {
//		int originNodeID = originNode.getId();
//		//Initialization
//
//		MyMinPriorityQueue<Node> Q = new MyMinPriorityQueue<Node>();
//		HashSet<Integer> Qbuddy = new HashSet<Integer>(this.getNumNodes()/3); //default initial capacity is one third of network size
//		HashSet<Integer> destinations = new HashSet<Integer>();
//
//		initializeDijkstraMinPriorityQueue(Q, Qbuddy, destinations, originNodeID, originNode);
//
//		while (!destinations.isEmpty()) {
//			//This is the neat thing about the priority queue. "polling" takes out the minimum element (the one with lowest distance) 
//			Node u = Q.poll();
//			destinations.remove(u.getId());
//			u.dijkstraVisitied = true;
//
//			for (int vID: u.getNeighbours()) {
//				Node v = this.getNode(vID);
//				if (v.dijkstraVisitied) {
//					continue;
//				}
//				if (!Qbuddy.contains(vID)) {
//					Qbuddy.add(vID);
//					Q.add(v);
//				}
//				double alt = u.dijkstraDist + this.getEdge(u,v).getGenCost(); 
//				if (alt < v.dijkstraDist) {
//					v.dijkstraDist = alt;
//					v.dijkstraPrev = u;
//					//	Q.remove(v);
//					//	Q.add(v);
//					//Updating in the priority queue and sifting up is now efficiently (O(log(n))) 
//					//implemented in {@code MyMinPriorityQueue} with a hashmap by @author mesch
//					Q.updateHeapPositionUp(v);
//				}
//			}
//		}
//
//		return true;
//	}
//	
//	
//	
//	
//	
//	/*Below is where the search for local detours is done*/
//	
//	
//	
//	/* Although the path does not violate the global cost criterion, it might violate a local cost criterion.
//	   Since this is tested every time the path is expanded, it only needs to be checked for the subtours from any of the existing nodes to the new node.
//	   If the criterion is violated for any of the subtours, the path is discontinued. */
//	
//	double lengthOfSubtour = edgesNodePair.get(currentPath[currentPath.length-1]).get(v).getGenCost();
//	/*First if is comparing to shortest path to previous node visited. Note that dijkstraDists refers to a hashmap with shortest paths between all node pairs (previously generated)*/
//	if(lengthOfSubtour <= dijkstraDists.get(currentPath[currentPath.length-1]).get(v) * localMaximumCostRatio){
//		/*if not violated, then loop through all previous nodes visited - potentially until origin*/
//		for(int i = currentPath.length  - 2; i >= 0; i--){
//			lengthOfSubtour += edgesNodePair.get(currentPath[i]).get(currentPath[i+1]).getGenCost();
//			if( lengthOfSubtour > dijkstraDists.get(currentPath[i]).get(v) * localMaximumCostRatio ){
//				/*if local detour constraint violated, then break*/
//				localConstraintViolated = true;
//				break;
//			}
//		}
//	} else {
//		localConstraintViolated = true;
//	}
//	if(localConstraintViolated){
//		continue;
//	}
}
