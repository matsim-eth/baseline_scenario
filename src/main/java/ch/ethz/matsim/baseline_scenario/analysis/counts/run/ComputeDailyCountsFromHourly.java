package ch.ethz.matsim.baseline_scenario.analysis.counts.run;

import java.io.IOException;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.HourlyReferenceCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.HourlySimulationCountsListener;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.SimulationCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.utils.HourlyCountsAggregator;
import ch.ethz.matsim.baseline_scenario.analysis.counts.writers.CSVDailyCountsWriter;

public class ComputeDailyCountsFromHourly {
	static public void main(String[] args) throws IOException {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(args[0]);
		
		Collection<HourlyCountItem> items = new HourlyReferenceCountsReader(network).read(args[1]);
		new SimulationCountsReader(new HourlySimulationCountsListener(items)).read(args[2]);
		new CSVDailyCountsWriter(new HourlyCountsAggregator().aggregate(items)).write(args[3]);
	}
}
