package ch.ethz.matsim.baseline_scenario.config;

public class ScenarioConfig {
	public String baselinePath;
	public String baselinePrefix;
	
	public String config;
	public Double xCoord;
	public Double yCoord;
	public Double rangeKm;
	
	public int numberOfThreads = 0;

	public String prefix = "cut_";
	public String outputPath;

	public boolean useMinimumNetworkCache = true;
}
