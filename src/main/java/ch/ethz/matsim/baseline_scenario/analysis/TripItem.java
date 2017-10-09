package ch.ethz.matsim.baseline_scenario.analysis;

import org.matsim.api.core.v01.Coord;

public class TripItem {
	public Coord origin;
	public Coord destination;
	public double startTime;
	public double travelTime;
	public double networkDistance;
	public String mode;
	public String purpose;
	public boolean returning;

	
	public TripItem(Coord origin, Coord destination, double startTime, double travelTime, double networkDistance, String mode, String purpose, boolean returning) {
		this.origin = origin;
		this.destination = destination;
		this.startTime = startTime;
		this.travelTime = travelTime;
		this.networkDistance = networkDistance;
		this.mode = mode;
		this.purpose = purpose;
		this.returning = returning;
	}
}
