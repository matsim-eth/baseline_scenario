package ch.ethz.matsim.baseline_scenario.preparation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

public class SimplifyNetwork {
	public void run(Network network) {
		Collection<Node> candidateNodes = new HashSet<>();
		int round = 1;

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
			}
		}
	}

	static public void main(String[] args) {
		String networkInputPath = args[0];
		String networkOutputPath = args[1];

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkInputPath);

		new SimplifyNetwork().run(network);
		new NetworkWriter(network).write(networkOutputPath);
	}
}
