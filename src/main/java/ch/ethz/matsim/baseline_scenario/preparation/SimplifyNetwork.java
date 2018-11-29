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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class SimplifyNetwork {
	public void run(Network network, TransitSchedule schedule) {
		Collection<Node> candidateNodes = new HashSet<>();
		int round = 1;

		Map<Id<Link>, Collection<Id<Link>>> replacements = new HashMap<>();

		while (true) {
			candidateNodes.clear();

			for (Node node : network.getNodes().values()) {
				if (node.getInLinks().size() == 1 && node.getOutLinks().size() == 1) {
					Link startLink = node.getInLinks().values().iterator().next();
					Link endLink = node.getOutLinks().values().iterator().next();

					if (!startLink.equals(endLink)) {
						candidateNodes.add(node);
					}
				}
			}

			for (TransitStopFacility facility : schedule.getFacilities().values()) {
				Link facilityLink = network.getLinks().get(facility.getLinkId());
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
				network.removeNode(middleNode.getId());

				Node endNode = endLink.getToNode();
				startLink.setToNode(endNode);
				network.addLink(startLink);

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

		for (TransitStopFacility facility : schedule.getFacilities().values()) {
			if (!network.getLinks().containsKey(facility.getLinkId())) {
				throw new IllegalStateException();
			}
		}

		Map<Id<Link>, Id<Link>> invertedReplacements = new HashMap<>();

		for (Map.Entry<Id<Link>, Collection<Id<Link>>> entry : replacements.entrySet()) {
			for (Id<Link> replacedLink : entry.getValue()) {
				invertedReplacements.put(replacedLink, entry.getKey());
			}
		}

		for (TransitLine transitLine : schedule.getTransitLines().values()) {
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
	}

	static public void main(String[] args) {
		String networkInputPath = args[0];
		String scheduleInputPath = args[1];
		String networkOutputPath = args[2];
		String scheduleOutputPath = args[3];

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkInputPath);
		new TransitScheduleReader(scenario).readFile(scheduleInputPath);

		new SimplifyNetwork().run(scenario.getNetwork(), scenario.getTransitSchedule());

		new NetworkWriter(scenario.getNetwork()).write(networkOutputPath);
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(scheduleOutputPath);
	}
}
