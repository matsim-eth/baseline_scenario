package ch.ethz.matsim.baseline_scenario.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class TripItem {
	public Id<Person> personId;
	public int personTripId;
	public Coord origin;
	public Coord destination;
	public double startTime;
	public double travelTime;
	public double networkDistance;
	public String mode;
	public String purpose;
	public boolean returning;

	
	public TripItem(Id<Person> personId, int personTripId, Coord origin, Coord destination, double startTime, double travelTime, double networkDistance, String mode, String purpose, boolean returning) {
		this.personId = personId;
		this.personTripId = personTripId;
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
