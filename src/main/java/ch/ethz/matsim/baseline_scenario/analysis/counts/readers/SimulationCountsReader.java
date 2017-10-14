package ch.ethz.matsim.baseline_scenario.analysis.counts.readers;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class SimulationCountsReader {
	final private SimulationCountsListener countsListener;

	public SimulationCountsReader(SimulationCountsListener countsListener) {
		this.countsListener = countsListener;
	}

	public void read(String eventsPath) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(countsListener);
		new MatsimEventsReader(eventsManager).readFile(eventsPath);
	}
}
