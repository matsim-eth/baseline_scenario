package ch.ethz.matsim.baseline_scenario.config;

public class ZurichScenarioConfig {
	public String baselinePath;
	public String baselinePrefix = "switzerland_";

	public int numberOfThreads = 0;

	public String prefix = "zurich_";
	public String outputPath;

	public boolean useMinimumNetworkCache = true;
	
	public double centerX = Double.NaN;
	public double centerY = Double.NaN;
	public double scenarioRadius = Double.NaN;
}
