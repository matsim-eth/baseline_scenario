package ch.ethz.matsim.baseline_scenario.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

public class CSVTripWriter {
	final private Collection<TripItem> trips;
	final private String delimiter;

	public CSVTripWriter(Collection<TripItem> trips) {
		this(trips, ";");
	}

	public CSVTripWriter(Collection<TripItem> trips, String delimiter) {
		this.trips = trips;
		this.delimiter = delimiter;
	}

	public void write(String outputPath) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		for (TripItem trip : trips) {
			writer.write(formatTrip(trip) + "\n");
			writer.flush();
		}

		writer.flush();
		writer.close();
	}

	private String normalizeActivityType(String activityType) {
		return activityType.replaceAll("_[0-9]+$", "");
	}

	private String formatTrip(TripItem trip) {
		return String.join(delimiter,
				new String[] { String.valueOf(trip.origin.getX()), String.valueOf(trip.origin.getY()),
						String.valueOf(trip.destination.getX()), String.valueOf(trip.destination.getY()),
						String.valueOf(trip.startTime), String.valueOf(trip.travelTime),
						String.valueOf(trip.networkDistance), String.valueOf(trip.mode),
						normalizeActivityType(String.valueOf(trip.purpose)), String.valueOf(trip.returning) });
	}
}
