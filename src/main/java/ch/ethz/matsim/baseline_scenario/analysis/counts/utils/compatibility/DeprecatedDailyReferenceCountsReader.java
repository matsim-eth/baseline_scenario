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

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;

public class DeprecatedDailyReferenceCountsReader {
	final private Logger logger = Logger.getLogger(DeprecatedDailyReferenceCountsReader.class);

	final private String delimiter;
	final private Network network;

	public DeprecatedDailyReferenceCountsReader(Network network, String delimiter) {
		this.delimiter = delimiter;
		this.network = network;
	}

	public DeprecatedDailyReferenceCountsReader(Network network) {
		this(network, ";");
	}

	public Collection<DailyCountItem> read(String path) throws IOException {
		Collection<DailyCountItem> counts = new LinkedList<>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		String line = null;

		List<String> header = null;
		List<String> row = null;

		while ((line = reader.readLine()) != null) {
			row = Arrays.asList(line.trim().split(delimiter));

			if (row.size() > 1) {
				if (header == null) {
					header = row;
				} else {
					String countStationId = row.get(header.indexOf("countStationId"));
					Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("linkId")));

					if (network.getLinks().containsKey(linkId)) {
						int reference = Integer.parseInt(row.get(header.indexOf("counts")));
						counts.add(new DailyCountItem(linkId, reference, network.getLinks().get(linkId).getCoord(), countStationId));
					} else {
						logger.warn(String.format("Link %s for \"%s\" not found in network", linkId.toString(),
								row.get(header.indexOf("direction"))));
					}
				}
			}
		}

		reader.close();

		return counts;
	}
}
