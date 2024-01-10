package runScript;
 
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import auxiliary.*;
import choiceModel.*;
import network.*;
import refCostFun.*;

public class LocalDetour {

	public static void main(String[] args) throws IOException {
		String publicNetworkDirFromCalc = "O:/Public/Sharing-4233-TKRA-Projects/tkra/Mads Paulsen (Network)/TransportationNetworks-master/";


		
		String networkDirectory = publicNetworkDirFromCalc; //Define directory of networks
		boolean modelCongestion = true;//Model congestion or just free-flow travel time
		

		double bound=1.205 ;
		double maximumCostRatio = bound+0.2;  //remove all paths from universal choice set which has cost ratio (on route level) larger than maximumCostRatio
		if (modelCongestion = false) maximumCostRatio = bound;
		for (int j=1;j<=1;j++) {

			
			

			RefCostFun phi = new RefCostTauMin(bound); //lower reference cost for DUE replication
			RefCostFun omega = new RefCostTauMin(bound); //upper reference cost for DUE replication

			RUM rum = new BCM(omega); 

			rum.theta=0.03;
			
						
			for (int k=1;k<=1;k++) {

					
				// Scale used in local detour choice model - if =-1 then use same as in choice model

				rum.thetaLocalD=0.2;
				if (rum.thetaLocalD <= -1) 
					{rum.thetaLocalD=rum.theta;
					}
				double includeDetourednessReduction=1; 
				double localMaximumCostRatio = 1.051; //Bound in Local Detour part (+ 1 relative to paper)
				for (int q=1;q<=1;q++)
				{
					double localMaximumCostRatioCutOff = localMaximumCostRatio+0.25; //localMaximumCostRatioCutOff defines bound on relative cost difference in initial search for choice set to use
					if (modelCongestion = false) localMaximumCostRatioCutOff = localMaximumCostRatio;

					
				boolean useLocalStorage = false; //
				boolean useOnlyConsideredPaths = false;
				boolean exportConsideredPaths = true;
				boolean consEnumIte = false;

				for (int i=1;i<=1;i++) {
					if (i==2) {
						useOnlyConsideredPaths = true;
						exportConsideredPaths = false;
					}
					if (i==3) {
						useOnlyConsideredPaths = true;
						exportConsideredPaths = false;
						consEnumIte=true;
					}

					 String networkName = "AnaheimMod";
//					String networkName = "AnaheimMod1OD";
					//String networkName = "Anaheim";
					//String networkName = "AnaheimNoBPR";
					//String networkName = "AnaheimNoBPR1ODUELoad";

					boolean isNetworkBidirectional = false; //is the network defined as bidirectional or one-way streets?



					double maximumCostDeviation = -1;
					if(maximumCostDeviation>-1) {maximumCostRatio=100;}
					
					//boolean isNetworkBidirectional = true; //is the network defined as bidirectional or one-way streets?
					boolean printStatusOnTheGo = true; //Level of detail of println messages during calculation

					String localStorageDirectory = "C:/Projekter/Programming/Java/storageLocalDetour2/" + networkName + "/";
					new File(localStorageDirectory).mkdir();
					rum.betaLength=0.5*.3/1000; //anaheim

					rum.betaTime=1.0;
					double demandScale;
					demandScale = 1.0;
					Network network = new Network(networkDirectory + networkName, isNetworkBidirectional, demandScale); //Initialize network
					network.setMaximumCostRatio(maximumCostRatio);
					network.setMaximumCostDeviation(maximumCostDeviation);
					network.setLocalMaximumCostRatioCutOff(localMaximumCostRatioCutOff);
					network.setLocalMaximumCostRatio(localMaximumCostRatio);
					network.minimumFlowToBeConsideredUsed = 0; // Allows flows of 0 to be considered too. Switch to a value higher than 0 to make cut-off.
					network.setLocalStorageDirectory(localStorageDirectory);
					network.setUseLocalStorage(useLocalStorage);
					network.printStatusOnTheGo = printStatusOnTheGo;
				
					if(includeDetourednessReduction==0) {
						network.setDetourednessParameter(0);
					}
					else {
					network.setDetourednessParameter(localMaximumCostRatio);
					}
					RSUET routeChoiceModel = new RSUET(rum, phi,omega, demandScale, consEnumIte, bound,
							useOnlyConsideredPaths, exportConsideredPaths); 
					routeChoiceModel.maximumCostRatio = maximumCostRatio;
					routeChoiceModel.epsilon = 0.00005;
					routeChoiceModel.itmax = 300;
					routeChoiceModel.flowDepTravelTime = modelCongestion; 
					

					ConvergencePattern conv = routeChoiceModel.solve(network); //get network to equilibrium

					boolean printToFile = true; //specify if you want the output printed
					network.printCS=true; // SPecify if you want the output to include the detailed choice sets.
					String outputFolderCalc = "C:/Projekter/Programming/Java/outputsLocaldetourBCM2";
					String outputFolder = outputFolderCalc;
					if (printToFile){
						//network.printOutput(outputFolder, routeChoiceModel, conv);
						String outputNameHelp = networkName + "_"+ (double)Math.round(rum.theta* 1000d)/1000d + "_" + (double)Math.round(bound*10d)/10d + "_" + useOnlyConsideredPaths +"_"+consEnumIte + "_"+ (double)Math.round(localMaximumCostRatio*includeDetourednessReduction* 1000d)/1000d + "_"+ (double)Math.round(rum.thetaLocalD* 1000d)/1000d;
						network.printOutput(outputFolder, routeChoiceModel, conv, outputNameHelp);
					}
					if(i==1 & !exportConsideredPaths) {
						break;
					}
				}
				localMaximumCostRatio+= 0.4;
			}
			rum.theta+=0.01;
			}
		bound-= 1480.0;
		}
	}
}