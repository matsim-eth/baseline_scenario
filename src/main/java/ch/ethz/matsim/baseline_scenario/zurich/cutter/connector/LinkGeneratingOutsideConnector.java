package ch.ethz.matsim.baseline_scenario.zurich.cutter.connector;

import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 * "Connects" the scenario with the outside world:
 * 
 * <ul>
 * <li>Outside activities with a link ID get a corresponding facility for
 * constistency in the scenario.</li>
 * <li>Outside activities with only a coordinate get a corresponding facility
 * and a connection to the network. This connection has the purpose to make the
 * scenario compatible with default MATSim. Otherwise it is recommended to NOT
 * replan trips leading to or coming from an outside activity. However, if this
 * is allowed we want to make sure that a routing is possible.
 * </ul>
 */
public class LinkGeneratingOutsideConnector implements OutsideConnector {
	final private static Logger log = Logger.getLogger(LinkGeneratingOutsideConnector.class);
	final private Population population;

	public LinkGeneratingOutsideConnector(Population population) {
		this.population = population;
	}

	/* (non-Javadoc)
	 * @see ch.ethz.matsim.baseline_scenario.zurich.cutter.connector.OutsideConnector#run(org.matsim.facilities.ActivityFacilities, org.matsim.api.core.v01.network.Network, org.matsim.api.core.v01.network.Network)
	 */
	@Override
	public void run(ActivityFacilities facilities, Network network, Network roadNetwork) {
		// First, make sure that every activity has a coordinate, a link and a facility
		log.info("Checking that every activity has a coordinate, a link and a facility ...");

		OutsideFacilityAdapter facilityAdapter = new OutsideFacilityAdapter(facilities);

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;

						if (activity.getType().equals("outside")) {
							ActivityFacility facility;

							if (activity.getLinkId() != null) {
								facility = facilityAdapter
										.getFacility(roadNetwork.getLinks().get(activity.getLinkId()));
							} else {
								facility = facilityAdapter.getFacility(activity.getCoord());
							}

							activity.setCoord(facility.getCoord());
							activity.setLinkId(facility.getLinkId());
							activity.setFacilityId(facility.getId());
						}
					}
				}
			}
		}

		// Second, connect the new facilities with the road network
		log.info("Connecting new facilities with the road network ...");

		Collection<ActivityFacility> detachedFacilities = facilityAdapter.getDetachedFacilities();
		NetworkFactory networkFactory = network.getFactory();

		log.info("  Number of detached facilities: " + detachedFacilities.size());

		int numberOfCreatedLinks = 0;
		int numberOfCreatedNodes = 0;

		for (ActivityFacility facility : detachedFacilities) {
			Node loopNode = networkFactory.createNode(Id.createNodeId(facility.getLinkId().toString() + "n"),
					facility.getCoord());
			Link loopLink = networkFactory.createLink(facility.getLinkId(), loopNode, loopNode);
			Link nearestLink = NetworkUtils.getNearestLink(roadNetwork, facility.getCoord());

			Link forwardLink = networkFactory.createLink(Id.createLinkId(facility.getLinkId().toString() + "f"),
					loopNode, nearestLink.getFromNode());
			Link backwardLink = networkFactory.createLink(Id.createLinkId(facility.getLinkId().toString() + "b"),
					nearestLink.getToNode(), loopNode);

			loopLink.setAllowedModes(Collections.singleton("car"));
			forwardLink.setAllowedModes(Collections.singleton("car"));
			backwardLink.setAllowedModes(Collections.singleton("car"));

			loopLink.setCapacity(nearestLink.getCapacity());
			forwardLink.setCapacity(nearestLink.getCapacity());
			backwardLink.setCapacity(nearestLink.getCapacity());

			loopLink.setFreespeed(nearestLink.getFreespeed());
			forwardLink.setFreespeed(nearestLink.getFreespeed());
			backwardLink.setFreespeed(nearestLink.getFreespeed());

			loopLink.setNumberOfLanes(1.0);
			forwardLink.setNumberOfLanes(1.0);
			backwardLink.setNumberOfLanes(1.0);

			double distance = Math.max(1.0,
					CoordUtils.calcEuclideanDistance(facility.getCoord(), nearestLink.getCoord()));

			loopLink.setLength(1.0);
			forwardLink.setLength(distance);
			backwardLink.setLength(distance);

			network.addNode(loopNode);
			network.addLink(loopLink);
			network.addLink(forwardLink);
			network.addLink(backwardLink);

			numberOfCreatedNodes++;
			numberOfCreatedLinks += 3;
		}

		log.info("  Number of created links: " + numberOfCreatedLinks);
		log.info("  Number of created nodes: " + numberOfCreatedNodes);
	}
}
