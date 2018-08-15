package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils;

import ch.ethz.matsim.baseline_scenario.additional_traffic.AdditionalTrafficType;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacility;

public class SingleFreightTripUtils {
    public static Person createSingleTripAgent(Id id, String freight_type, Plan plan) {
        Person person = PopulationUtils.getFactory().createPerson(Id.create(id, Person.class));
        person.getAttributes().putAttribute( "subpopulation", AdditionalTrafficType.FREIGHT.toString());
        person.getAttributes().putAttribute( "freight_type", freight_type);
        person.addPlan(plan);
        return person;
    }

    public static Plan createSingleTripPlan(double departureTime, String activityType, String transportMode, ActivityFacility startFacility, ActivityFacility endFacility) {
        Plan plan = PopulationUtils.createPlan();

        Activity actStart = PopulationUtils.createActivityFromCoordAndLinkId(activityType, startFacility.getCoord(), startFacility.getLinkId());
        actStart.setFacilityId(startFacility.getId());
        actStart.setStartTime(0.0);
        actStart.setMaximumDuration(departureTime);
        actStart.setEndTime(departureTime);
        plan.addActivity(actStart);

        plan.addLeg(PopulationUtils.createLeg(transportMode));

        Activity actEnd = PopulationUtils.createActivityFromCoordAndLinkId(activityType, endFacility.getCoord(), endFacility.getLinkId());
        actEnd.setFacilityId(endFacility.getId());
        actEnd.setStartTime(departureTime);
        plan.addActivity(actEnd);
        return plan;
    }
}
