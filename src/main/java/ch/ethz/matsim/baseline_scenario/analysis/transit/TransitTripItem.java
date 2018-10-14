package ch.ethz.matsim.baseline_scenario.analysis.transit;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class TransitTripItem {
	public Id<Person> personId = null;
	public int personTripId = -1;

	public Coord origin = null;
	public Coord destination = null;

	public double startTime = 0.0;
	public double totTripTime = 0.0;

	public double inVehicleTime = 0.0;
	public double waitingTime = 0.0;
	public double transferTime = 0.0;

	public double inVehicleDistance = 0.0;
	public double inVehicleCrowflyDistance = 0.0;

	public double accessDistance = 0.0;
	public double egressDistance = 0.0;

	public double transferDistance = 0.0;

	public int numberOfTransfers = -1;

	public double firstWaitingTime = Double.NaN;

	public boolean fullMinibusTrip = true;
	public boolean partMinibusTrip = false;

	public boolean fullRefTrip = true;
	public boolean partRefTrip = false;

	public TransitTripItem() {
	}
}
