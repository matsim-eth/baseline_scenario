package ch.ethz.matsim.baseline_scenario.config;

public class SwitzerlandConfig {
	public String inputPath;
	public String outputPath;

	public double inputDownsampling;
	public double outputScenarioScale;

	public int numberOfThreads;

	public String prefix = "baseline_";

	public boolean performIterativeLocationChoice = false;
}
