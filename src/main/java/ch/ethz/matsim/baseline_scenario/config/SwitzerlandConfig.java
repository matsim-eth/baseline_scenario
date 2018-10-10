package ch.ethz.matsim.baseline_scenario.config;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.config.FreightConfig;

public class SwitzerlandConfig {
	public String inputPath;
	public String outputPath;

	public double inputDownsampling = 1.0;
	public double outputScenarioScale = 1.0;

	public int numberOfThreads = 0;

	public String prefix = "switzerland_";

	public boolean performIterativeLocationChoice = false;

	public boolean includeFreight = true;
	public FreightConfig freightConfig = new FreightConfig();
}
