package ch.ethz.matsim.baseline_scenario.analysis.counts.writers;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;

public class CSVHourlyCountsWriter {
	final private Collection<HourlyCountItem> items;

	public CSVHourlyCountsWriter(Collection<HourlyCountItem> items) {
		this.items = items;
	}

	public void write(String outputPath) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		writer.write(formatHeader() + "\n");
		writer.flush();

		for (HourlyCountItem item : items) {
			writer.write(formatItem(item) + "\n");
			writer.flush();
		}

		writer.flush();
		writer.close();
	}

	private String formatHeader() {
		return String.join(";",
				new String[] { "link", "hour", "location_x", "location_y", "reference_count", "simulation_count" });
	}

	private String formatItem(HourlyCountItem item) {
		return String.join(";",
				new String[] { item.link.toString(), String.valueOf(item.hour), String.valueOf(item.location.getX()),
						String.valueOf(item.location.getY()), String.valueOf(item.reference),
						String.valueOf(item.simulation) });
	}
}
