package ch.ethz.matsim.baseline_scenario.zurich.cutter.utils;

import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class ScheduleEndTimeFinder {
	public double findScheduleEndTime(TransitSchedule transitSchedule) {
		double timeAfterSchedule = 0.0;

		for (TransitLine line : transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				double departure = route.getDepartures().values().stream().mapToDouble(d -> d.getDepartureTime()).max()
						.orElse(0.0);

				for (TransitRouteStop stop : route.getStops()) {
					double departureOffset = stop.getDepartureOffset();
					double arrivalOffset = stop.getArrivalOffset();

					if (Double.isFinite(departureOffset)) {
						timeAfterSchedule = Math.max(timeAfterSchedule, departure + departureOffset);
					}

					if (Double.isFinite(arrivalOffset)) {
						timeAfterSchedule = Math.max(timeAfterSchedule, departure + arrivalOffset);
					}
				}
			}
		}

		return timeAfterSchedule;
	}
}
