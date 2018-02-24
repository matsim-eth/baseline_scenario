package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points;

import java.util.List;

import org.matsim.core.population.routes.NetworkRoute;

public interface NetworkCrossingPointFinder {
	List<NetworkCrossingPoint> findCrossingPoints(NetworkRoute route, double departureTime);
}