package ch.ethz.matsim.baseline_scenario.analysis.counts.utils.compatibility;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.CountItem;

public class CountNetworkMapper {
	final private Logger logger = Logger.getLogger(CountNetworkMapper.class);

	final private Network deprecatedNetwork;
	final private QuadTree<Link> quadtree;

	public CountNetworkMapper(Network deprecatedNetwork, Network currentNetwork) {
		this.deprecatedNetwork = deprecatedNetwork;

		double[] dimensions = NetworkUtils.getBoundingBox(currentNetwork.getNodes().values());
		quadtree = new QuadTree<>(dimensions[0], dimensions[1], dimensions[2], dimensions[3]);
		currentNetwork.getLinks().values().forEach(l -> quadtree.put(l.getCoord().getX(), l.getCoord().getY(), l));
	}

	public void run(Collection<? extends CountItem> countItems) {
		double maximumDistance = 500.0;

		Iterator<? extends CountItem> iterator = countItems.iterator();
		Map<Link, Link> assigned = new HashMap<>();

		while (iterator.hasNext()) {
			CountItem item = iterator.next();

			Link deprecatedLink = deprecatedNetwork.getLinks().get(item.link);
			if (assigned.containsKey(deprecatedLink)) {
				item.link = assigned.get(deprecatedLink).getId();
				continue;
			}

			Coord referenceFromCoord = deprecatedLink.getFromNode().getCoord();
			Coord referenceToCoord = deprecatedLink.getToNode().getCoord();
			double referenceAngle = Math.atan2(referenceToCoord.getY() - referenceFromCoord.getY(),
					referenceToCoord.getX() - referenceFromCoord.getX());

			List<Link> candidates = new LinkedList<>(
					quadtree.getDisk(item.location.getX(), item.location.getY(), maximumDistance));

			if (candidates.size() > 0) {
				List<Double> scores = candidates.stream().map(c -> {
					Coord fromCoord = c.getFromNode().getCoord();
					Coord toCoord = c.getToNode().getCoord();
					double angle = Math.atan2(toCoord.getY() - fromCoord.getY(), toCoord.getX() - fromCoord.getX());
					
					double distance = CoordUtils.calcEuclideanDistance(item.location, c.getCoord());					
					double angularDistance = Math.abs(referenceAngle - angle);

					return distance + angularDistance;
				}).collect(Collectors.toList());

				int bestIndex = 0;
				double bestScore = scores.get(0);

				for (int i = 1; i < candidates.size(); i++) {
					if (scores.get(i) < bestScore) {
						bestScore = scores.get(i);
						bestIndex = i;
					}
				}

				Link candidate = candidates.get(bestIndex);

				if (!assigned.containsValue(candidate)) {
					logger.info("Update " + deprecatedLink.getId().toString() + " -> " + candidate.getId().toString());
					item.link = candidate.getId();
					assigned.putIfAbsent(deprecatedLink, candidate);
				} else {
					logger.warn("Best candidate for " + item.link.toString() + " is already assigned.");
					iterator.remove();
				}
			} else {
				logger.warn("No link found for " + item.link.toString());
				iterator.remove();
			}
		}
	}
}
