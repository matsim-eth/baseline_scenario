package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points;

import java.util.List;

import org.matsim.pt.routes.ExperimentalTransitRoute;

public interface TransitRouteCrossingPointFinder {

	List<TransitRouteCrossingPoint> findCrossingPoints(ExperimentalTransitRoute route, double departureTime);

}