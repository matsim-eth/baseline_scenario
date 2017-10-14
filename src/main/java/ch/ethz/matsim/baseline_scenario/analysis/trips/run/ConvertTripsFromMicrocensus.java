package ch.ethz.matsim.baseline_scenario.analysis.trips.run;

import java.io.IOException;
import java.util.Collection;

import ch.ethz.matsim.baseline_scenario.analysis.trips.CSVTripWriter;
import ch.ethz.matsim.baseline_scenario.analysis.trips.TripItem;
import ch.ethz.matsim.baseline_scenario.analysis.trips.readers.MicrocensusTripReader;

public class ConvertTripsFromMicrocensus {
	static public void main(String[] args) throws NumberFormatException, IOException {
		Collection<TripItem> trips = new MicrocensusTripReader().readTrips(args[0]);
		new CSVTripWriter(trips).write(args[1]);
	}
}
