package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points;

import java.util.List;

import ch.ethz.matsim.baseline_scenario.transit.routing.EnrichedTransitRoute;

public interface TransitRouteCrossingPointFinder {
	List<TransitRouteCrossingPoint> findCrossingPoints(EnrichedTransitRoute route, double departureTime);
}