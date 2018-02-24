package ch.ethz.matsim.baseline_scenario.zurich.utils;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.facilities.ActivityFacility;

public interface ActivityWithFacility extends Activity {
	ActivityFacility getFacility();
}
