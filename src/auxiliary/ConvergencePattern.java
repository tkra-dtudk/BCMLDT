package auxiliary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

import choiceModel.RSUET;
import network.Network;

public class ConvergencePattern {
	/**
	 * the number of iterations saved in this 
	 * convergence pattern
	 */
	int size;

	/**
	 * {@code true} if the algorithm that this convergence
	 * pattern refers to converged, {@code false} otherwise.
	 */
	public boolean didConverge = false;

	/**
	 * The default initial capacity of the ArrayLists
	 * used to store gap information (50).
	 */
	private static final int DEFAULT_INITIAL_CAPACITY = 50;

	ArrayList<Integer> iterationNumber;
	String networkName;
	ArrayList<Double> relGapUnusedBelow;
	ArrayList<Double> relGapUnusedAbove;
	ArrayList<Double> relGapUsed;

	/**
	 * The time elapsed since creation of the convergence pattern, in ms
	 */
	ArrayList<Double> time;

	/**
	 * StopWatch object used to time the convergence
	 */
	StopWatch timer = new StopWatch();

	/**
	 * delimiter used by the print method.
	 * 
	 * @see ConvergencePattern#printToFile(File)
	 */
	private String delimiter = ";";

	/**
	 * @return the value of {@linkplain ConvergencePattern#didConverge}
	 */
	public boolean didConverge() {
		return this.didConverge;
	}

	/**
	 * Initializes an empty convergence pattern.
	 */
	public ConvergencePattern() {
		iterationNumber = new ArrayList<Integer>(DEFAULT_INITIAL_CAPACITY);
		relGapUnusedBelow = new ArrayList<Double>(DEFAULT_INITIAL_CAPACITY);
		relGapUnusedAbove = new ArrayList<Double>(DEFAULT_INITIAL_CAPACITY);
		relGapUsed = new ArrayList<Double>(DEFAULT_INITIAL_CAPACITY);
		time = new ArrayList<Double>(DEFAULT_INITIAL_CAPACITY);
		size = 0;
		timer.start(); //start the stop watch
	}

	/**
	 * Initializes a convergence pattern with a name.
	 */
	public ConvergencePattern(String networkName) {
		this.networkName = networkName;
		iterationNumber = new ArrayList<Integer>(DEFAULT_INITIAL_CAPACITY);
		relGapUnusedBelow = new ArrayList<Double>(DEFAULT_INITIAL_CAPACITY);
		relGapUnusedAbove = new ArrayList<Double>(DEFAULT_INITIAL_CAPACITY);
		relGapUsed = new ArrayList<Double>(DEFAULT_INITIAL_CAPACITY);
		time = new ArrayList<Double>(DEFAULT_INITIAL_CAPACITY);
		size = 0;
		timer.start(); //start the stop watch
	}

	/**
	 * Prints gap measures in the TMNL
	 * @see RSUET#cutUniversalChoiceSetAlgorithm(Network)
	 * @param iterationNumber the iteration number to print
	 * @param gapUnusedAbove the unused gap above to print
	 * @param gapUnusedBelow the unused gap below to print
	 * @param gapUsed the used gap to print
	 */
	public void printCurrentIteration() {
		int index = this.size - 1;
		String seperator = "    ";
		String gapFormat = "%7.5f";
		System.out.printf("%-6s"+gapFormat + seperator + gapFormat
				+ seperator + gapFormat + seperator + "%07.0f" + "\n", 
				iterationNumber.get(index)+")",relGapUnusedAbove.get(index),relGapUnusedBelow.get(index),
				relGapUsed.get(index),time.get(index));
	}

	/**
	 * Prints the header which corresponds to the gap measure
	 */
	public void printGapHeader() {
		String it = "it";
		String gapAbove = "gapAbove";
		String gapBelow = "gapBelow";
		String gapUsed =  "gapUsed ";
		String time =     "time [ms]";
		System.out.printf("%-6s%-11s%-11s%-11s%-11s%n", it, gapAbove, gapBelow, gapUsed, time);
	}

	public void addIteration(int iterationNumber, double relGapUnusedBelow, double relGapUnusedAbove, double relGapUsed) {
		this.iterationNumber.add(iterationNumber);
		this.relGapUnusedBelow.add(relGapUnusedBelow);
		this.relGapUnusedAbove.add(relGapUnusedAbove);
		this.relGapUsed.add(relGapUsed);
		this.time.add(timer.lap());
		size ++;
	}


	public void printToFile(File file) throws FileNotFoundException {
		printToFile(file,this.delimiter);
	}

	public void printToFile(File file, String delim) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(file);
		//print header
		out.print("It");
		out.print(delim);
		out.print("gapUsedBelow");
		out.print(delim);
		out.print("gapUsedAbove");
		out.print(delim);
		out.print("gapUsed");
		out.print(delim);
		out.print("time [ms]");
		out.println();

		for (int index = 0; index < size; index++) {
			out.print(iterationNumber.get(index));
			out.print(delim);
			out.print(relGapUnusedBelow.get(index));
			out.print(delim);
			out.print(relGapUnusedAbove.get(index));
			out.print(delim);
			out.print(relGapUsed.get(index));
			out.print(delim);
			out.print(time.get(index));
			out.println();
		}
		out.close();
	}

}
