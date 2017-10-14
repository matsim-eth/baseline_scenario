package ch.ethz.matsim.baseline_scenario.analysis.counts.run;

import java.io.IOException;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.DailyReferenceCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.DailySimulationCountsListener;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.SimulationCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.writers.CSVDailyCountsWriter;

public class ComputeDailyCounts {
	static public void main(String[] args) throws IOException {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(args[0]);
		
		Collection<DailyCountItem> items = new DailyReferenceCountsReader(network).read(args[1]);
		new SimulationCountsReader(new DailySimulationCountsListener(items)).read(args[2]);
		new CSVDailyCountsWriter(items).write(args[3]);
	}
}
