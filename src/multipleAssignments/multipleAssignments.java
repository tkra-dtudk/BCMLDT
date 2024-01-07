package multipleAssignments;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import auxiliary.*;
import choiceModel.*;
import network.*;
import refCostFun.*;
public class multipleAssignments {

	public static void main(String[] args) throws IOException {
		String publicNetworkDirFromCalc = "O:/Public/DMC/temp/Mads Paulsen (Network)/TransportationNetworks-master/";
		String publicNetworkDirFromPC = "O:/Public/DMC/temp/Mads Paulsen (Network)/TransportationNetworks-master/";
		String localNetworkDir = "C:/Projekter/Programming/Java/TransportationNetworks-master/";

		String networkDirectory = publicNetworkDirFromCalc; //Define directory of networks
		
		Boolean useLocalStorage = false; //
		
		
		
		String networkName = "SiouxFalls"; //Choose network
//		String networkName = "Anaheim";
	//	String networkName = "Berlin-Friedrichshain"; //Choose network
		double maximumCostRatio = 1.4;
		double localMaximumCostRatio = 2.2;
		boolean isNetworkBidirectional = false; //is the network defined as bidirectional or one-way streets?
		boolean printStatusOnTheGo = false; //Level of detail of println messages during calculation

		String localStorageDirectory = "C:/Projekter/Programming/Java/storage/" + networkName + "/";
		new File(localStorageDirectory).mkdir();
		
		double demandScale;
		demandScale = 1.0;
		Network network = new Network(networkDirectory + networkName, isNetworkBidirectional, demandScale); //Initialize network
		network.setMaximumCostRatio(maximumCostRatio);
		network.setLocalMaximumCostRatioCutOff(localMaximumCostRatio);
		network.minimumFlowToBeConsideredUsed = 0; // Allows flows of 0 to be considered too. Switch to a value higher than 0 to make cut-off.
		network.setLocalStorageDirectory(localStorageDirectory);
		network.setUseLocalStorage(useLocalStorage);
		network.printStatusOnTheGo = printStatusOnTheGo;
		
		boolean printToFile = true; //specify if you want the output printed
		String outputFolderPC = "O:/Public/DMC/temp/Mads Paulsen (Network)/Outputs";
		String outputFolderCalc = "C:/Projekter/Programming/Java/Outputs";
		String outputFolder = outputFolderCalc;

		
		RefCostFun phi = new RefCostTauMin(1.3); //lower reference cost in RSUET
		RefCostFun omega = new RefCostTauMin(1.3); //upper reference cost in RSUET 
		
		RSUET.doInitialRSUET = 0;
		 
//		phi = omega; // make phi = omega; this is computationally demanding. Only do it on small networks. 
		
		for (double x=2;x<=2;x=x+1) {
			System.out.print("value of x :" + x/10d);
			System.out.print("\n");
		
			RUM rum = new TMNL(omega); //TMNL random utility model -- explicitly state to use ref cost omega 
			
			rum.theta=x/10d;
			
		//	RSUET routeChoiceModel = new RSUET(rum, phi,omega, demandScale); //set up the TMNL RSUET(min, min + 10)
		RSUET routeChoiceModel = new RSUET(rum, phi,omega, demandScale, printToFile, x, printToFile, printToFile); //set up the TMNL RSUET(min, min + 10)
			
			routeChoiceModel.maximumCostRatio = maximumCostRatio;
			routeChoiceModel.epsilon = 0.00005;


		
		ConvergencePattern conv = routeChoiceModel.solve(network); //get network to equilibrium
		RSUET.laterIteration=1; //Help, only do initial RSUET assignemtn in first iteration.
		if (printToFile){
			network.printOutput(outputFolder, routeChoiceModel, conv);
		}
		}


		
	}
}
