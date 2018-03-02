package ch.ethz.matsim.baseline_scenario.transit.connection;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public interface TransitConnectionFinder {
	TransitConnection findConnection(double departureTime, double totalTravelTime, Id<TransitStopFacility> accessStopId,
			Id<TransitStopFacility> egressStopId, TransitRoute transitRoute) throws NoConnectionFoundException;

	public static class NoConnectionFoundException extends Exception {
		private static final long serialVersionUID = -4938361475494542684L;
	}
}
