package ch.ethz.matsim.baseline_scenario.analysis.readers;

import java.util.Collection;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import ch.ethz.matsim.baseline_scenario.analysis.TripItem;
import ch.ethz.matsim.baseline_scenario.analysis.listeners.TripListener;

public class EventsTripReader {
	final private TripListener tripListener;
	
	public EventsTripReader(TripListener tripListener) {
		this.tripListener = tripListener;
	}
	
	public Collection<TripItem> readTrips(String eventsPath) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(tripListener);
		new MatsimEventsReader(eventsManager).readFile(eventsPath);
		return tripListener.getTripItems();
	}
}
