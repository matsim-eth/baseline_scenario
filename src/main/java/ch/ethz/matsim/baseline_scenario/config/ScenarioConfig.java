package ch.ethz.matsim.baseline_scenario.config;

public class ScenarioConfig {
	public String baselinePath;
	public String baselinePrefix;
	
	public String config;
	public Double xCoord;
	public Double yCoord;
	public Double rangeMeter;
	
	public int numberOfThreads = 0;

	public String prefix = "cut_";
	public String outputPath;

	public boolean useMinimumNetworkCache = true;
	
	public boolean useSwissRailRaptor = true;
	
	public boolean cutPopulationAttributes = true;
	public boolean cutFacilities = true;
	public boolean cutNetwork = true;
	public boolean cutHouseholds = true;
	public boolean cutHouseholdAttributes = true;
	public boolean cutTransit = true;
	
}
