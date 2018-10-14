package ch.ethz.matsim.baseline_scenario.analysis.transit.readers;

import java.util.Collection;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

import ch.ethz.matsim.baseline_scenario.analysis.transit.TransitTripItem;
import ch.ethz.matsim.baseline_scenario.analysis.transit.listeners.TransitTripListener;
import ch.ethz.matsim.baseline_scenario.transit.events.PublicTransitEvent;
import ch.ethz.matsim.baseline_scenario.transit.events.PublicTransitEventMapper;

public class EventsTransitTripReader {
	final private TransitTripListener tripListener;

	public EventsTransitTripReader(TransitTripListener tripListener) {
		this.tripListener = tripListener;
	}

	public Collection<TransitTripItem> readTrips(String eventsPath) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(tripListener);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
		//reader.addCustomEventMapper(PublicTransitEvent.TYPE, new PublicTransitEventMapper());
		reader.readFile(eventsPath);

		return tripListener.getTransitTripItems();
	}
}
