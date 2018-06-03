package ch.ethz.matsim.baseline_scenario.analysis.counts.run;

import java.io.IOException;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.utils.compatibility.DeprecatedHourlyReferenceCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.writers.MATSimHourlyReferenceWriter;

public class WriteHourlyReference {
	static public void main(String[] args) throws IOException {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(args[0]);
		
		Collection<HourlyCountItem> items = new DeprecatedHourlyReferenceCountsReader(network).read(args[1]);
		new MATSimHourlyReferenceWriter(items).write(args[2]);
	}
}
