package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.pt.routes.ExperimentalTransitRoute;

public class DefaultTransitTripCrossingPointFinder implements TransitTripCrossingPointFinder {
	final private TransitRouteCrossingPointFinder transitFinder;
	final private TeleportationCrossingPointFinder walkFinder;

	public DefaultTransitTripCrossingPointFinder(TransitRouteCrossingPointFinder transitFinder,
			TeleportationCrossingPointFinder walkFinder) {
		this.transitFinder = transitFinder;
		this.walkFinder = walkFinder;
	}

	@Override
	public List<TransitTripCrossingPoint> findCrossingPoints(Coord startCoord, List<PlanElement> trip, Coord endCoord) {
		List<TransitTripCrossingPoint> result = new LinkedList<>();

		for (int i = 0; i < trip.size(); i++) {
			PlanElement element = trip.get(i);

			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				switch (leg.getMode()) {
				case "transit_walk":
				case "access_walk":
				case "egress_walk":
					Coord legStartCoord = (i == 0) ? startCoord : ((Activity) trip.get(i - 1)).getCoord();
					Coord legEndCoord = (i == trip.size() - 1) ? endCoord : ((Activity) trip.get(i + 1)).getCoord();

					result.addAll(walkFinder
							.findCrossingPoints(legStartCoord, legEndCoord, leg.getTravelTime(), leg.getDepartureTime())
							.stream().map(p -> new TransitTripCrossingPoint(p)).collect(Collectors.toList()));
					break;
				case "pt":
					result.addAll(transitFinder
							.findCrossingPoints((ExperimentalTransitRoute) leg.getRoute(), leg.getDepartureTime())
							.stream().map(p -> new TransitTripCrossingPoint(p)).collect(Collectors.toList()));
					break;
				default:
					throw new IllegalStateException();
				}
			}
		}

		return result;
	}
}
