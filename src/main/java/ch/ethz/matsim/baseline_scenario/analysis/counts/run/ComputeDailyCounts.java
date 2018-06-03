package ch.ethz.matsim.baseline_scenario.analysis.counts.run;

import java.io.IOException;
import java.util.Collection;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.CSVDailyCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.DailySimulationCountsListener;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.SimulationCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.writers.CSVDailyCountsWriter;

public class ComputeDailyCounts {
	static public void main(String[] args) throws IOException {
		Collection<DailyCountItem> items = new CSVDailyCountsReader().read(args[0]);
		new SimulationCountsReader(new DailySimulationCountsListener(items)).read(args[1]);
		new CSVDailyCountsWriter(items).write(args[2]);
	}
}
