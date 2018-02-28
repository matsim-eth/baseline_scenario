package ch.ethz.matsim.baseline_scenario.transit;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;

public class DefaultEnrichedTransitRouter implements EnrichedTransitRouter {
	final private TransitRouter delegate;
	final private TransitSchedule transitSchedule;
	final private DepartureFinder departureFinder;
	final private Network network;
	final private double beelineDistanceFactor;
	final private double additionalTransferTime;

	public DefaultEnrichedTransitRouter(TransitRouter delegate, TransitSchedule transitSchedule,
			DepartureFinder departureFinder, Network network, double beelineDistanceFactor,
			double additionalTransferTime) {
		this.delegate = delegate;
		this.transitSchedule = transitSchedule;
		this.departureFinder = departureFinder;
		this.network = network;
		this.beelineDistanceFactor = beelineDistanceFactor;
		this.additionalTransferTime = additionalTransferTime;
	}

	@Override
	public List<Leg> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {
		List<Leg> legs = delegate.calcRoute(fromFacility, toFacility, departureTime, person);
		double currentTime = departureTime;

		for (int i = 0; i < legs.size(); i++) {
			if (legs.get(i).getMode().equals("pt")) {
				PublicTransitLegInfo legInfo = computePublicTransitLegInfo(legs.get(i), currentTime);

				legs.get(i).getAttributes().putAttribute("expectedInVehicleTime", legInfo.inVehicleTime);
				legs.get(i).getAttributes().putAttribute("expectedWaitingTime", legInfo.waitingTime);

				legs.get(i).getRoute().setDistance(legInfo.distance);
				legs.get(i).setDepartureTime(currentTime);
			} else if (legs.get(i).getMode().contains("walk") && !legs.get(i).getMode().equals("walk")) {
				Coord originCoord = (i == 0) ? fromFacility.getCoord()
						: transitSchedule.getFacilities()
								.get(((ExperimentalTransitRoute) legs.get(i - 1).getRoute()).getEgressStopId())
								.getCoord();

				Coord destinationCoord = (i == legs.size() - 1) ? toFacility.getCoord()
						: transitSchedule.getFacilities()
								.get(((ExperimentalTransitRoute) legs.get(i + 1).getRoute()).getAccessStopId())
								.getCoord();

				double beelineDistance = CoordUtils.calcEuclideanDistance(originCoord, destinationCoord);
				double distance = beelineDistance * beelineDistanceFactor;

				legs.get(i).getRoute().setDistance(distance);
				legs.get(i).setDepartureTime(currentTime);
			} else {
				throw new IllegalStateException("Can only enrich pt and *_walk legs");
			}

			currentTime += legs.get(i).getTravelTime();
		}

		return legs;
	}

	private class PublicTransitLegInfo {
		final public double inVehicleTime;
		final public double waitingTime;
		final public double distance;

		PublicTransitLegInfo(double inVehicleTime, double waitingTime, double distance) {
			this.inVehicleTime = inVehicleTime;
			this.waitingTime = waitingTime;
			this.distance = distance;
		}
	}

	private int updateAccessStopIndex(TransitRoute transitRoute, Id<TransitStopFacility> stopFacilityId,
			double minimumDepartureTime, int accessStopIndex, int egressStopIndex) {
		for (int i = egressStopIndex - 1; i > accessStopIndex; i--) {
			TransitRouteStop stop = transitRoute.getStops().get(i);

			if (stop.getStopFacility().getId().equals(stopFacilityId)) {
				if (departureFinder.findDeparture(transitRoute, stop, minimumDepartureTime) != null) {
					return i;
				}
			}
		}

		return accessStopIndex;
	}

	private int findStopIndex(TransitRoute transitRoute, Id<TransitStopFacility> stopFacilityId,
			double minimumDepartureTime, int minimumIndex) {

		for (int i = minimumIndex; i < transitRoute.getStops().size(); i++) {
			TransitRouteStop stop = transitRoute.getStops().get(i);

			if (stop.getStopFacility().getId().equals(stopFacilityId)) {
				if (departureFinder.findDeparture(transitRoute, stop, minimumDepartureTime) != null) {
					return i;
				}
			}
		}

		throw new IllegalStateException("Cannot find stop facility " + stopFacilityId + " on route "
				+ transitRoute.getId() + " after " + Time.writeTime(minimumDepartureTime));
	}

	private PublicTransitLegInfo computePublicTransitLegInfo(Leg leg, double legDepartureTime) {
		ExperimentalTransitRoute originalRoute = (ExperimentalTransitRoute) leg.getRoute();
		legDepartureTime += additionalTransferTime;

		TransitRoute transitRoute = transitSchedule.getTransitLines().get(originalRoute.getLineId()).getRoutes()
				.get(originalRoute.getRouteId());

		// Find the first stop with the given access stop id with a departure after the
		// leg departure time
		int accessStopIndex = findStopIndex(transitRoute, originalRoute.getAccessStopId(), legDepartureTime, 0);
		TransitRouteStop accessStop = transitRoute.getStops().get(accessStopIndex);

		// Find the corresponding departure
		Departure routeDeparture = departureFinder.findDeparture(transitRoute, accessStop, legDepartureTime);
		double vehicleDepartureTime = accessStop.getDepartureOffset() + routeDeparture.getDepartureTime();

		// Find the stop with the given egress stop id that comes after the access stop
		// and after the vehicle departure time
		int egressStopIndex = findStopIndex(transitRoute, originalRoute.getEgressStopId(), vehicleDepartureTime,
				accessStopIndex);
		TransitRouteStop egressStop = transitRoute.getStops().get(egressStopIndex);

		// Compute waiting time
		double inVehicleTime = egressStop.getArrivalOffset() - accessStop.getDepartureOffset();
		double waitingTime = originalRoute.getTravelTime() - inVehicleTime;
		double distance = RouteUtils.calcDistance(originalRoute, transitSchedule, network);

		if (waitingTime < 0.0) {
			// It may happen that the route has a loop. A good indicator for that is that
			// the waiting time is negative. In that case we can try to recover the actual
			// access stop (which must come after the one that we initially found and before
			// the egress stop).

			accessStopIndex = updateAccessStopIndex(transitRoute, originalRoute.getAccessStopId(), legDepartureTime, accessStopIndex,
					egressStopIndex);
			accessStop = transitRoute.getStops().get(accessStopIndex);
		}

		inVehicleTime = egressStop.getArrivalOffset() - accessStop.getDepartureOffset();
		waitingTime = originalRoute.getTravelTime() - inVehicleTime;
		distance = RouteUtils.calcDistance(originalRoute, transitSchedule, network);

		if (inVehicleTime < 0.0) {
			throw new IllegalStateException();
		}

		if (waitingTime < 0.0) {
			throw new IllegalStateException();
		}

		return new PublicTransitLegInfo(inVehicleTime, waitingTime, distance);
	}
}
