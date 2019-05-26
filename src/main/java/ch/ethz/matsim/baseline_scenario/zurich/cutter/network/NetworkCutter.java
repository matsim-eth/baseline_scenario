package ch.ethz.matsim.baseline_scenario.zurich.cutter.network;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class NetworkCutter {
	final private static Logger log = Logger.getLogger(NetworkCutter.class);

	final private ScenarioExtent extent;
	final private MinimumNetworkFinder minimumNetworkFinder;

	public NetworkCutter(ScenarioExtent extent, MinimumNetworkFinder minimumNetworkFinder) {
		this.extent = extent;
		this.minimumNetworkFinder = minimumNetworkFinder;
	}

	public void run(Population population, TransitSchedule transitSchedule, Network network) {
		int originalNumberOfLinks = network.getLinks().size();
		int originalNumberOfNodes = network.getNodes().size();
		log.info("Cutting the network ...");

		// Collect all links that within the area
		Set<Id<Link>> retainedInsideLinkIds = new HashSet<>();
		Set<Id<Link>> routeSearchLinkIds = new HashSet<>();

		for (Link link : network.getLinks().values()) {
			if (extent.isInside(link.getToNode().getCoord()) || extent.isInside(link.getFromNode().getCoord())) {
				retainedInsideLinkIds.add(link.getId());
			}

			if (link.getId().toString().contains("outside")) {
				retainedInsideLinkIds.add(link.getId());
				routeSearchLinkIds.add(link.getId());
			}
		}

		// Collect all links that are needed by the population
		Set<Id<Link>> retainedPopulationLinkIds = new HashSet<>();

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;

						if (!activity.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
							retainedPopulationLinkIds.add(activity.getLinkId());
							routeSearchLinkIds.add(activity.getLinkId());
						}
					} else {
						Leg leg = (Leg) element;
						Route route = leg.getRoute();

						if (route instanceof NetworkRoute) {
							NetworkRoute networkRoute = (NetworkRoute) route;

							retainedPopulationLinkIds.add(networkRoute.getStartLinkId());
							retainedPopulationLinkIds.add(networkRoute.getEndLinkId());
							retainedPopulationLinkIds.addAll(networkRoute.getLinkIds());
							
							routeSearchLinkIds.add(networkRoute.getStartLinkId());
							routeSearchLinkIds.add(networkRoute.getEndLinkId());
							routeSearchLinkIds.addAll(networkRoute.getLinkIds());
						}
					}
				}
			}
		}

		// Collect all links that are needed by the public transit lines
		Set<Id<Link>> retainedPublicTransitLinkIds = new HashSet<>();

		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				NetworkRoute networkRoute = transitRoute.getRoute();

				retainedPublicTransitLinkIds.add(networkRoute.getStartLinkId());
				retainedPublicTransitLinkIds.add(networkRoute.getEndLinkId());
				retainedPublicTransitLinkIds.addAll(networkRoute.getLinkIds());
			}
		}

		for (TransitStopFacility facility : transitSchedule.getFacilities().values()) {
			retainedPublicTransitLinkIds.add(facility.getLinkId());
		}

		// Further processing is needed for the population links, because it may be the
		// case that an agent is using "walk" in the given population, whereas he may
		// change this decision in the simulation. Then, we still need to make sure that
		// his trips can be performed with "car". Technically, this means that we define
		// a common point (e.g. a link around Bellevue) and make sure that there exists
		// a route from all retained links to this point and that all retained links can
		// be reached by this reference point.

		Set<Id<Link>> allRetainedLinkIds = new HashSet<>();
		allRetainedLinkIds.addAll(minimumNetworkFinder.run(routeSearchLinkIds));
		allRetainedLinkIds.addAll(retainedInsideLinkIds);
		allRetainedLinkIds.addAll(retainedPublicTransitLinkIds);
		allRetainedLinkIds.addAll(retainedPopulationLinkIds);

		// Note that this means, that public transit lines CANNOT change their routes in
		// the simulation if this is desired (at least not outside of the scenario
		// extent).

		Set<Id<Node>> allRetainedNodeIds = new HashSet<>();

		for (Id<Link> linkId : allRetainedLinkIds) {
			allRetainedNodeIds.add(network.getLinks().get(linkId).getFromNode().getId());
			allRetainedNodeIds.add(network.getLinks().get(linkId).getToNode().getId());
		}

		Set<Id<Link>> allRemovedLinkIds = new HashSet<>(network.getLinks().keySet());
		Set<Id<Node>> allRemovedNodeIds = new HashSet<>(network.getNodes().keySet());

		allRemovedLinkIds.removeAll(allRetainedLinkIds);
		allRemovedNodeIds.removeAll(allRetainedNodeIds);

		//allRemovedLinkIds.forEach(id -> network.removeLink(id));
		//allRemovedNodeIds.forEach(id -> network.removeNode(id));
		
		for (Id<Link> linkId : allRemovedLinkIds) {
			network.getLinks().get(linkId).getAttributes().putAttribute("remove", "true");
		}
		
		for (Id<Node> nodeId : allRemovedNodeIds) {
			network.getNodes().get(nodeId).getAttributes().putAttribute("remove", "true");
		}

		int finalNumberOfNodes = network.getNodes().size();
		int finalNumberOfLinks = network.getLinks().size();

		log.info("Finished cutting the network.");
		log.info("  Number of nodes before: " + originalNumberOfNodes);
		log.info("  Number of links before: " + originalNumberOfLinks);
		log.info("  Number of nodes now: " + finalNumberOfNodes);
		log.info("  Number of links now: " + finalNumberOfLinks);
	}
}
