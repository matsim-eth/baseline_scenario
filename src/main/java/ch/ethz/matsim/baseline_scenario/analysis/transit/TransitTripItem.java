package ch.ethz.matsim.baseline_scenario.analysis.transit;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class TransitTripItem {
	public Id<Person> personId = null;
	public int personTripId = -1;

	public Coord origin = null;
	public Coord destination = null;
	public double crowflyDistance;

	public double startTime = 0.0;

	public double inVehicleTime = 0.0;
	public double waitingTime = 0.0;
	public double transferTime = 0.0;

	public double inVehicleDistance = 0.0;
	public double inVehicleCrowflyDistance = 0.0;

	public double transferDistance = 0.0;
	public double transferCrowflyDistance = 0.0;

	public int numberOfTransfers = -1;

	public TransitTripItem() {
	}

	public TransitTripItem(Id<Person> personId, int personTripId, Coord origin, Coord destination, double startTime,
			double inVehicleTime, double waitingTime, double inVehicleDistance, double inVehicleCrowflyDistance,
			double transferDistance, double transferCrowflyDistance, int numberOfTransfers) {
		this.personId = personId;
		this.personTripId = personTripId;
		this.origin = origin;
		this.destination = destination;
		this.startTime = startTime;
		this.inVehicleTime = inVehicleTime;
		this.waitingTime = waitingTime;
		this.inVehicleDistance = inVehicleDistance;
		this.inVehicleCrowflyDistance = inVehicleCrowflyDistance;
		this.transferDistance = transferDistance;
		this.transferCrowflyDistance = transferCrowflyDistance;
		this.numberOfTransfers = numberOfTransfers;
	}
}
