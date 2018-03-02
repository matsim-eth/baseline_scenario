package ch.ethz.matsim.baseline_scenario.zurich.cutter.utils;

import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public class DefaultDepartureFinder implements DepartureFinder {
	@Override
	public Departure findDeparture(TransitRoute route, TransitRouteStop accessStop, double departureTime)
			throws NoDepartureFoundException {
		double accessStopOffset = accessStop.getDepartureOffset();

		Departure result = route.getDepartures().values().stream()
				.filter(d -> departureTime <= d.getDepartureTime() + accessStopOffset)
				.min((a, b) -> Double.compare(a.getDepartureTime(), b.getDepartureTime())).orElse(null);

		if (result == null) {
			throw new NoDepartureFoundException();
		}

		return result;
	}
}
