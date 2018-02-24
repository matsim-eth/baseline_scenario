package ch.ethz.matsim.baseline_scenario.zurich.router.trip;

import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.ScheduleEndTimeFinder;
import ch.ethz.matsim.baseline_scenario.zurich.utils.ActivityWithFacility;

public class PublicTransitTripRouter implements TripRouter {
	final private TripRouterWithRoutingModule delegate;
	final private TransitSchedule transitSchedule;
	final private DepartureFinder departureFinder;
	final private ModeRoutingParams walkParams;
	final private double timeAfterSchedule;

	public PublicTransitTripRouter(Network network, TransitSchedule transitSchedule, TransitRouter transitRouter,
			ModeRoutingParams walkParams, DepartureFinder departureFinder) {
		this.transitSchedule = transitSchedule;
		this.departureFinder = departureFinder;
		this.walkParams = walkParams;

		RoutingModule walkRouter = new TeleportationRoutingModule("transit_walk", PopulationUtils.getFactory(),
				walkParams.getTeleportedModeSpeed(), walkParams.getBeelineDistanceFactor());

		this.delegate = new TripRouterWithRoutingModule(
				new TransitRouterWrapper(transitRouter, transitSchedule, network, walkRouter));

		this.timeAfterSchedule = new ScheduleEndTimeFinder().findScheduleEndTime(transitSchedule);
	}

	@Override
	public List<PlanElement> route(ActivityWithFacility originActivity, List<PlanElement> trip,
			ActivityWithFacility destinationActivity) {
		List<PlanElement> result = delegate.route(originActivity, trip, destinationActivity);

		List<Coord> locations = result.stream().filter(e -> (e instanceof Activity)).map(e -> ((Activity) e).getCoord())
				.collect(Collectors.toList());
		locations.add(0, originActivity.getCoord());
		locations.add(destinationActivity.getCoord());

		// The MATSim router does not guarantee that departure times are given

		double currentTime = originActivity.getEndTime();

		for (int i = 0; i < result.size(); i += 2) {
			Leg leg = (Leg) result.get(i);

			switch (leg.getMode()) {
			case "transit_walk":
			case "access_walk":
			case "egress_walk":
				double beelineDistance = CoordUtils.calcEuclideanDistance(locations.get(i / 2),
						locations.get(i / 2 + 1));
				double walkDistance = beelineDistance * walkParams.getBeelineDistanceFactor();

				leg.setDepartureTime(currentTime);
				leg.getRoute().setDistance(walkDistance);
				currentTime += leg.getTravelTime();
				break;
			case "pt":
				ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();

				TransitRoute transitRoute = transitSchedule.getTransitLines().get(route.getLineId()).getRoutes()
						.get(route.getRouteId());

				TransitRouteStop accessStop = transitRoute
						.getStop(transitSchedule.getFacilities().get(route.getAccessStopId()));

				Departure departure = departureFinder.findDeparture(transitRoute, accessStop, currentTime);

				if (departure == null) {
					currentTime = Math.max(timeAfterSchedule, currentTime + 3600.0);
				} else {
					currentTime = departure.getDepartureTime() + accessStop.getDepartureOffset();
				}

				leg.setDepartureTime(currentTime);
				currentTime += leg.getTravelTime();
				break;
			default:
				throw new IllegalStateException();
			}
		}

		return result;
	}

}
