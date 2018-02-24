package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class DefaultTeleportationCrossingPointFinder implements TeleportationCrossingPointFinder {
	final private ScenarioExtent extent;

	public DefaultTeleportationCrossingPointFinder(ScenarioExtent extent) {
		this.extent = extent;
	}

	@Override
	public List<TeleportationCrossingPoint> findCrossingPoints(Coord originCoord, Coord destinationCoord,
			double originalTravelTime, double departureTime) {
		double originalDistance = CoordUtils.calcEuclideanDistance(originCoord, destinationCoord);

		List<Coord> crossingCoords = extent.computeCrowflyCrossings(originCoord, destinationCoord);
		List<TeleportationCrossingPoint> crossingPoints = new LinkedList<>();

		boolean isOutgoing = extent.isInside(originCoord);

		for (Coord crossingCoord : crossingCoords) {
			double updatedDistance = CoordUtils.calcEuclideanDistance(crossingCoord, destinationCoord);
			double updatedTravelTime = originalTravelTime - originalTravelTime * (updatedDistance / originalDistance);

			crossingPoints
					.add(new TeleportationCrossingPoint(crossingCoord, departureTime + updatedTravelTime, isOutgoing));
			isOutgoing = !isOutgoing;
		}

		return crossingPoints;
	}
}
