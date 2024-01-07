package auxiliary;

public class StopWatch {
	private double startTime;
	
	public StopWatch(){
		startTime = -1;
	}
	
	public void start(){
		startTime = System.currentTimeMillis();
	}
	
	public double stop() throws RuntimeException{
		double elapsedTime = lap();
		startTime = -1;
		return elapsedTime;
	}
	
	public double lap() throws RuntimeException{
		if (startTime==-1) throw new RuntimeException("The StopWatch object must be started before it can stop!");
		return System.currentTimeMillis() - startTime;
	}
}