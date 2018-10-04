package ch.ethz.matsim.baseline_scenario.config;

import org.matsim.api.core.v01.Coord;

import ch.ethz.matsim.baseline_scenario.zurich.extent.CircularScenarioExtent;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class ZurichScenarioConfig {
	public String baselinePath;
	public String baselinePrefix = "switzerland_";

	public int numberOfThreads = 0;

	public String prefix = "zurich_";
	public String outputPath;

	public boolean useMinimumNetworkCache = true;
	
	public double centerX = 2683253.0;
	public double centerY = 1246745.0;
	public double radius = 30000.0;
}