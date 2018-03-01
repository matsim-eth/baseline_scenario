package ch.ethz.matsim.baseline_scenario.zurich.cutter.connector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 * "Connects" the scenario with the outside world:
 * 
 * <ul>
 * <li>Outside activities with a link ID get a corresponding facility for
 * constistency in the scenario.</li>
 * <li>Outside activities with only a coordinate get a corresponding facility
 * which is located at the closest road network link,
 * </ul>
 */
public class ClosestLinkOutsideConnector implements OutsideConnector {
	final private static Logger log = Logger.getLogger(ClosestLinkOutsideConnector.class);
	final private Population population;

	public ClosestLinkOutsideConnector(Population population) {
		this.population = population;
	}

	@Override
	public void run(ActivityFacilities facilities, Network network, Network roadNetwork) {
		log.info("Checking that every activity has a coordinate, a link and a facility ...");

		OutsideFacilityAdapter facilityAdapter = new OutsideFacilityAdapter(facilities);

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;

						if (activity.getType().equals("outside")) {
							Link activityLink;

							if (activity.getLinkId() != null) {
								activityLink = roadNetwork.getLinks().get(activity.getLinkId());
							} else {
								activityLink = NetworkUtils.getNearestLink(roadNetwork, activity.getCoord());
							}

							ActivityFacility facility = facilityAdapter.getFacility(activityLink);

							activity.setCoord(facility.getCoord());
							activity.setLinkId(facility.getLinkId());
							activity.setFacilityId(facility.getId());
						}
					}
				}
			}
		}

		if (facilityAdapter.getDetachedFacilities().size() > 0) {
			throw new IllegalStateException();
		}
	}
}