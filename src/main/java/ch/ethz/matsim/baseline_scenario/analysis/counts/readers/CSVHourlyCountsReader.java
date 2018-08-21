package ch.ethz.matsim.baseline_scenario.analysis.counts.readers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;

public class CSVHourlyCountsReader {
	public Collection<HourlyCountItem> read(String path) throws IOException {
		List<HourlyCountItem> items = new LinkedList<>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

		List<String> header = null;
		String line = null;

		while ((line = reader.readLine()) != null) {
			List<String> row = Arrays.asList(line.split(";"));

			if (header == null) {
				header = row;
			} else {
				String countStationId = row.get(header.indexOf("countStationId"));
				Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("link")));
				int referenceCount = Integer.parseInt(row.get(header.indexOf("reference_count")));
				int simulationCount = Integer.parseInt(row.get(header.indexOf("simulation_count")));
				double x = Double.parseDouble(row.get(header.indexOf("location_x")));
				double y = Double.parseDouble(row.get(header.indexOf("location_y")));
				Coord location = new Coord(x, y);
				int hour = Integer.parseInt(row.get(header.indexOf("hour")));

				HourlyCountItem item = new HourlyCountItem(linkId, hour, referenceCount, location, countStationId);
				item.simulation = simulationCount;

				items.add(item);
			}
		}

		reader.close();
		return items;
	}
}
