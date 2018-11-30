package ch.ethz.matsim.baseline_scenario.preparation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;

public class SimplifyNetwork {
	public void run(Scenario scenario) {
		Collection<Node> candidateNodes = new HashSet<>();
		int round = 1;

		Map<Id<Link>, Collection<Id<Link>>> replacements = new HashMap<>();

		while (true) {
			candidateNodes.clear();

			for (Node node : scenario.getNetwork().getNodes().values()) {
				if (node.getInLinks().size() == 1 && node.getOutLinks().size() == 1) {
					Link startLink = node.getInLinks().values().iterator().next();
					Link endLink = node.getOutLinks().values().iterator().next();

					if (!startLink.equals(endLink)) {
						candidateNodes.add(node);
					}
				}
			}

			for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
				Link facilityLink = scenario.getNetwork().getLinks().get(facility.getLinkId());
				candidateNodes.remove(facilityLink.getFromNode());
			}

			if (candidateNodes.size() == 0) {
				break;
			}

			System.out.println(String.format("Round %d: Removing %d nodes and links", round, candidateNodes.size()));
			round++;

			while (candidateNodes.size() > 0) {
				Node middleNode = candidateNodes.iterator().next();

				Link startLink = middleNode.getInLinks().values().iterator().next();
				Link endLink = middleNode.getOutLinks().values().iterator().next();

				startLink.setLength(startLink.getLength() + endLink.getLength());
				startLink.setCapacity(Math.max(startLink.getCapacity(), endLink.getCapacity()));
				startLink.setFreespeed(Math.max(startLink.getFreespeed(), endLink.getFreespeed()));
				startLink.setNumberOfLanes(Math.max(startLink.getNumberOfLanes(), endLink.getNumberOfLanes()));

				Set<String> modes = new HashSet<>();
				modes.addAll(startLink.getAllowedModes());
				modes.addAll(endLink.getAllowedModes());
				startLink.setAllowedModes(modes);

				// This will remove the framing links, too.
				scenario.getNetwork().removeNode(middleNode.getId());

				Node endNode = endLink.getToNode();
				startLink.setToNode(endNode);
				scenario.getNetwork().addLink(startLink);

				Node startNode = startLink.getFromNode();

				candidateNodes.remove(middleNode);
				candidateNodes.remove(startNode);
				candidateNodes.remove(endNode);

				startLink.getAttributes().putAttribute("isCollapsed", true);

				// Register that startLink replaces endLink
				if (!replacements.containsKey(startLink.getId())) {
					replacements.put(startLink.getId(), new HashSet<>());
				}
				replacements.get(startLink.getId()).add(endLink.getId());

				// If endLink already replaces other links, they are now also replaced by
				// startLink
				if (replacements.containsKey(endLink.getId())) {
					replacements.get(startLink.getId()).addAll(replacements.get(endLink.getId()));
					replacements.remove(endLink.getId());
				}
			}
		}

		for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
			if (!scenario.getNetwork().getLinks().containsKey(facility.getLinkId())) {
				throw new IllegalStateException();
			}
		}

		Map<Id<Link>, Id<Link>> invertedReplacements = new HashMap<>();

		for (Map.Entry<Id<Link>, Collection<Id<Link>>> entry : replacements.entrySet()) {
			for (Id<Link> replacedLink : entry.getValue()) {
				invertedReplacements.put(replacedLink, entry.getKey());
			}
		}

		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<Id<Link>> linkIds = new LinkedList<>();
				linkIds.add(transitRoute.getRoute().getStartLinkId());
				linkIds.addAll(transitRoute.getRoute().getLinkIds());
				linkIds.add(transitRoute.getRoute().getEndLinkId());

				linkIds = linkIds.stream()
						.map(id -> invertedReplacements.containsKey(id) ? invertedReplacements.get(id) : id)
						.collect(Collectors.toList());

				linkIds = new ArrayList<>(new LinkedHashSet<>(linkIds));

				Id<Link> startLinkId = linkIds.remove(0);
				Id<Link> endLinkId = linkIds.size() == 0 ? startLinkId : linkIds.remove(linkIds.size() - 1);

				transitRoute.getRoute().setLinkIds(startLinkId, linkIds, endLinkId);
			}
		}

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;
						activity.setLinkId(
								invertedReplacements.getOrDefault(activity.getLinkId(), activity.getLinkId()));
					}

					if (element instanceof Leg) {
						Leg leg = (Leg) element;
						Route route = leg.getRoute();

						if (route instanceof NetworkRoute) {
							NetworkRoute networkRoute = (NetworkRoute) route;

							List<Id<Link>> linkIds = new LinkedList<>();
							linkIds.add(networkRoute.getStartLinkId());
							linkIds.addAll(networkRoute.getLinkIds());
							linkIds.add(networkRoute.getEndLinkId());

							linkIds = linkIds.stream()
									.map(id -> invertedReplacements.containsKey(id) ? invertedReplacements.get(id) : id)
									.collect(Collectors.toList());

							linkIds = new ArrayList<>(new LinkedHashSet<>(linkIds));

							Id<Link> startLinkId = linkIds.remove(0);
							Id<Link> endLinkId = linkIds.size() == 0 ? startLinkId : linkIds.remove(linkIds.size() - 1);

							networkRoute.setLinkIds(startLinkId, linkIds, endLinkId);
						} else if (route instanceof AbstractRoute) {
							AbstractRoute abstractRoute = (AbstractRoute) route;
							abstractRoute.setStartLinkId(invertedReplacements
									.getOrDefault(abstractRoute.getStartLinkId(), abstractRoute.getStartLinkId()));
							abstractRoute.setEndLinkId(invertedReplacements.getOrDefault(abstractRoute.getEndLinkId(),
									abstractRoute.getEndLinkId()));
						} else {
							throw new IllegalStateException();
						}
					}
				}
			}
		}

		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			ActivityFacilityImpl implFacility = (ActivityFacilityImpl) facility;
			implFacility
					.setLinkId(invertedReplacements.getOrDefault(implFacility.getLinkId(), implFacility.getLinkId()));
		}
	}

	static public void main(String[] args) {
		String configPath = args[0];
		String networkOutputPath = args[1];
		String scheduleOutputPath = args[2];
		String populationOutputPath = args[3];
		String facilitiesOutputPath = args[4];

		Config config = ConfigUtils.loadConfig(configPath);

		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		new SimplifyNetwork().run(scenario);

		new NetworkWriter(scenario.getNetwork()).write(networkOutputPath);
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(scheduleOutputPath);
		new PopulationWriter(scenario.getPopulation()).write(populationOutputPath);
		new FacilitiesWriter(scenario.getActivityFacilities()).write(facilitiesOutputPath);
	}
}
