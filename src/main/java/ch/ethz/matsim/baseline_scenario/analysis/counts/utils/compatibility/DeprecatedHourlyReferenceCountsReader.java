package ch.ethz.matsim.baseline_scenario.analysis.counts.utils.compatibility;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;

public class DeprecatedHourlyReferenceCountsReader {
	final private Logger logger = Logger.getLogger(DeprecatedHourlyReferenceCountsReader.class);

	final private String delimiter;
	final private Network network;

	public DeprecatedHourlyReferenceCountsReader(Network network, String delimiter) {
		this.delimiter = delimiter;
		this.network = network;
	}

	public DeprecatedHourlyReferenceCountsReader(Network network) {
		this(network, ";");
	}

	public Collection<HourlyCountItem> read(String path) throws IOException {
		Collection<HourlyCountItem> counts = new LinkedList<>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		String line = null;

		List<String> header = null;
		List<String> row = null;

		while ((line = reader.readLine()) != null) {
			row = Arrays.asList(line.trim().split(delimiter));

			if (header == null) {
				header = row;
			} else {
				Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("linkId")));
				int midnightIndex = header.indexOf("count");
				if (midnightIndex == -1)
					midnightIndex = header.indexOf("count_h1");

				if (network.getLinks().containsKey(linkId)) {
					for (int i = 0; i < 24; i++) {
						int reference = Integer.parseInt(row.get(midnightIndex + i));
						counts.add(
								new HourlyCountItem(linkId, i, reference, network.getLinks().get(linkId).getCoord()));
					}
				} else {
					logger.warn(String.format("Link %s for \"%s\" not found in network", linkId.toString(),
							row.get(header.indexOf("direction"))));
				}
			}
		}

		reader.close();

		return counts;
	}
}
