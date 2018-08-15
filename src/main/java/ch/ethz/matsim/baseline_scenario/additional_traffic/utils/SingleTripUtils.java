package ch.ethz.matsim.baseline_scenario.additional_traffic.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.facilities.ActivityFacility;

public interface SingleTripUtils {
    Person createSingleTripAgent(Id id, Plan plan);
    Plan createSingleTripPlan(double departureTime, String activityType, String transportMode,
                              ActivityFacility startFacility, ActivityFacility endFacility);
}
