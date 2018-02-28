package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points;

import java.util.LinkedList;
import java.util.List;

import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.ScheduleEndTimeFinder;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

/*
 * TODO: This class should make use of StopSequenceCrossingPointFinder
 */
public class DefaultTransitRouteCrossingPointFinder implements TransitRouteCrossingPointFinder {
	final private ScenarioExtent extent;
	final private TransitSchedule schedule;
	final private DepartureFinder departureFinder;
	final private double timeAfterSchedule;

	public DefaultTransitRouteCrossingPointFinder(ScenarioExtent extent, TransitSchedule schedule,
			DepartureFinder departureFinder) {
		this.extent = extent;
		this.schedule = schedule;
		this.departureFinder = departureFinder;
		this.timeAfterSchedule = new ScheduleEndTimeFinder().findScheduleEndTime(schedule);
	}

	@Override
	public List<TransitRouteCrossingPoint> findCrossingPoints(ExperimentalTransitRoute route, double departureTime) {
		List<TransitRouteCrossingPoint> crossingPoints = new LinkedList<>();

		TransitLine transitLine = schedule.getTransitLines().get(route.getLineId());
		TransitRoute transitRoute = transitLine.getRoutes().get(route.getRouteId());

		int index = 0;
		int accessStopIndex = -1;
		int egressStopIndex = -1;
		
		// TODO: Important! Here we also need to consider the departure times. See DefaultEnrichedTransitRouter!

		while (index < transitRoute.getStops().size() && accessStopIndex == -1) {
			TransitRouteStop stop = transitRoute.getStops().get(index);

			if (stop.getStopFacility().getId().equals(route.getAccessStopId())) {
				accessStopIndex = index;
			}

			index++;
		}

		while (index < transitRoute.getStops().size() && egressStopIndex == -1) {
			TransitRouteStop stop = transitRoute.getStops().get(index);

			if (stop.getStopFacility().getId().equals(route.getEgressStopId())) {
				egressStopIndex = index;
			}

			index++;
		}

		if (accessStopIndex == -1 || egressStopIndex == -1) {
			throw new IllegalStateException("Could not find access and egress stop on the specified route");
		}

		double routeDepartureTime = timeAfterSchedule;

		Departure departure = departureFinder.findDeparture(transitRoute, transitRoute.getStops().get(accessStopIndex),
				departureTime);

		if (departure != null) {
			routeDepartureTime = departure.getDepartureTime();
		}

		List<TransitRouteStop> stops = transitRoute.getStops().subList(accessStopIndex, egressStopIndex + 1);

		for (int i = 0; i < stops.size() - 1; i++) {
			TransitRouteStop firstStop = stops.get(i);
			TransitRouteStop secondStop = stops.get(i + 1);

			boolean firstIsInside = extent.isInside(firstStop.getStopFacility().getCoord());
			boolean secondIsInside = extent.isInside(secondStop.getStopFacility().getCoord());

			if (firstIsInside != secondIsInside) { // We found a crossing
				TransitRouteStop insideStop = firstIsInside ? firstStop : secondStop;
				TransitRouteStop outsideStop = firstIsInside ? secondStop : firstStop;

				double outsideDepartureTime = routeDepartureTime + outsideStop.getDepartureOffset();
				double insideDepartureTime = routeDepartureTime + insideStop.getDepartureOffset();

				crossingPoints.add(new TransitRouteCrossingPoint(transitLine, transitRoute, outsideStop, insideStop,
						outsideDepartureTime, insideDepartureTime, firstIsInside));
			}
		}

		return crossingPoints;
	}
}
