package ch.ethz.matsim.baseline_scenario.analysis.transit.listeners;

import org.matsim.api.core.v01.Coord;

import ch.ethz.matsim.baseline_scenario.analysis.transit.TransitTripItem;

public class TransitTripListenerItem extends TransitTripItem {
	public String mode = "unknown";
	public Coord intermediateOrigin;
	public double legDepartureTime;
	public boolean waitingForFirstTransitEvent = true;
}
