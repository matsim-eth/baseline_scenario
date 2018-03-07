package ch.ethz.matsim.baseline_scenario.transit.connection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder.NoDepartureFoundException;

public class DefaultTransitConnectionFinder implements TransitConnectionFinder {
	final private DepartureFinder departureFinder;

	public DefaultTransitConnectionFinder(DepartureFinder departureFinder) {
		this.departureFinder = departureFinder;
	}

	private int updateAccessStopIndex(TransitRoute transitRoute, Id<TransitStopFacility> stopFacilityId,
			double minimumDepartureTime, int accessStopIndex, int egressStopIndex) {
		for (int i = egressStopIndex - 1; i > accessStopIndex; i--) {
			TransitRouteStop stop = transitRoute.getStops().get(i);

			if (stop.getStopFacility().getId().equals(stopFacilityId)) {
				try {
					departureFinder.findDeparture(transitRoute, stop, minimumDepartureTime);
					return i; // Return if a departure is found
				} catch (NoDepartureFoundException e) {
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
				try {
					departureFinder.findDeparture(transitRoute, stop, minimumDepartureTime);
					return i; // Return if a departure is found
				} catch (NoDepartureFoundException e) {
				}
			}
		}

		throw new IllegalStateException("Cannot find stop facility " + stopFacilityId + " on route "
				+ transitRoute.getId() + " after " + Time.writeTime(minimumDepartureTime));
	}

	@Override
	public TransitConnection findConnection(double connectionDepartureTime, double totalTravelTime,
			Id<TransitStopFacility> accessStopId, Id<TransitStopFacility> egressStopId, TransitRoute transitRoute)
			throws NoConnectionFoundException {
		try {
			// Find the first stop with the given access stop id with a departure after the
			// leg departure time
			int accessStopIndex = findStopIndex(transitRoute, accessStopId, connectionDepartureTime, 0);
			TransitRouteStop accessStop = transitRoute.getStops().get(accessStopIndex);

			// Find the corresponding departure
			Departure routeDeparture = departureFinder.findDeparture(transitRoute, accessStop, connectionDepartureTime);
			double vehicleDepartureTime = accessStop.getDepartureOffset() + routeDeparture.getDepartureTime();

			// Find the stop with the given egress stop id that comes after the access stop
			// and after the vehicle departure time
			int egressStopIndex = findStopIndex(transitRoute, egressStopId, vehicleDepartureTime, accessStopIndex);
			TransitRouteStop egressStop = transitRoute.getStops().get(egressStopIndex);

			// Compute waiting time
			double inVehicleTime = egressStop.getArrivalOffset() - accessStop.getDepartureOffset();
			double waitingTime = totalTravelTime - inVehicleTime;

			if (waitingTime < 0.0) {
				// It may happen that the route has a loop. A good indicator for that is that
				// the waiting time is negative. In that case we can try to recover the actual
				// access stop (which must come after the one that we initially found and before
				// the egress stop).

				accessStopIndex = updateAccessStopIndex(transitRoute, accessStopId, connectionDepartureTime,
						accessStopIndex, egressStopIndex);
				accessStop = transitRoute.getStops().get(accessStopIndex);

				// Find the corresponding departure
				routeDeparture = departureFinder.findDeparture(transitRoute, accessStop, connectionDepartureTime);
				vehicleDepartureTime = accessStop.getDepartureOffset() + routeDeparture.getDepartureTime();
			}

			inVehicleTime = egressStop.getArrivalOffset() - accessStop.getDepartureOffset();
			waitingTime = totalTravelTime - inVehicleTime;

			if (inVehicleTime < 0.0 || waitingTime < 0.0) {
				throw new IllegalStateException(String.format(
						"In-vehicle time or waiting time is negative! Route: %s, Access stop: %s, Egress stop: %s, Departure time: %f (Access stop index: %d, Egress stop index: %d, In-vehicle time: %f, Waiting time: %f)",
						transitRoute.getId().toString(), accessStopId.toString(), egressStopId.toString(),
						connectionDepartureTime, accessStopIndex, egressStopIndex, inVehicleTime, waitingTime));
			}

			return new DefaultTransitConnection(routeDeparture, accessStop, egressStop, inVehicleTime, waitingTime);
		} catch (NoDepartureFoundException e) {
			throw new NoConnectionFoundException();
		}
	}
}
