package ch.ethz.matsim.baseline_scenario.analysis.utils;

public class BaselineHomeActivityTypes implements HomeActivityTypes {
	@Override
	public boolean isHomeActivity(String activityType) {
		return activityType.contains("home");
	}
}
