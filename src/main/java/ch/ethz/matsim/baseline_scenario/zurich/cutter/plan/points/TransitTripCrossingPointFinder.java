package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.PlanElement;

public interface TransitTripCrossingPointFinder {

	List<TransitTripCrossingPoint> findCrossingPoints(Coord startCoord, List<PlanElement> trip, Coord endCoord);

}
