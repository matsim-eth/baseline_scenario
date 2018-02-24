package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points;

import java.util.List;

import org.matsim.api.core.v01.Coord;

public interface TeleportationCrossingPointFinder {

	List<TeleportationCrossingPoint> findCrossingPoints(Coord originCoord, Coord destinationCoord,
			double originalTravelTime, double departureTime);

}