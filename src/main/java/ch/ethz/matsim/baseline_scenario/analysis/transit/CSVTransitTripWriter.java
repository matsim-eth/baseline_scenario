package ch.ethz.matsim.baseline_scenario.analysis.transit;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

public class CSVTransitTripWriter {
	final private Collection<TransitTripItem> trips;
	final private String delimiter;

	public CSVTransitTripWriter(Collection<TransitTripItem> trips) {
		this(trips, ";");
	}

	public CSVTransitTripWriter(Collection<TransitTripItem> trips, String delimiter) {
		this.trips = trips;
		this.delimiter = delimiter;
	}

	public void write(String outputPath) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		writer.write(formatHeader() + "\n");
		writer.flush();

		for (TransitTripItem trip : trips) {
			writer.write(formatTrip(trip) + "\n");
			writer.flush();
		}

		writer.flush();
		writer.close();
	}

	private String formatHeader() {
		return String.join(delimiter,
				new String[] { "person_id", "person_trip_id", "start_time", "trip_time", "first_waiting_time",
						"waiting_time", "in_vehicle_time", "in_vehicle_distance", "in_vehicle_crowfly_distance",
						"transfer_time", "transfer_distance", "number_of_transfers", "access_distance",
						"egress_distance", "fullMinibusTrip", "partMinibusTrip", "fullRefTrip", "partRefTrip"	});
	}

	private String formatTrip(TransitTripItem trip) {
		return String.join(delimiter, new String[] { trip.personId.toString(), String.valueOf(trip.personTripId),
				String.valueOf(trip.startTime), String.valueOf(trip.totTripTime), String.valueOf(trip.firstWaitingTime),
				String.valueOf(trip.waitingTime),
				String.valueOf(trip.inVehicleTime), String.valueOf(trip.inVehicleDistance), String.valueOf(trip.inVehicleCrowflyDistance),
				String.valueOf(trip.transferTime), String.valueOf(trip.transferDistance), String.valueOf(trip.numberOfTransfers),
				String.valueOf(trip.accessDistance), String.valueOf(trip.egressDistance), String.valueOf(trip.fullMinibusTrip),
				String.valueOf(trip.partMinibusTrip), String.valueOf(trip.fullRefTrip), String.valueOf(trip.partRefTrip)	});
	}
}
