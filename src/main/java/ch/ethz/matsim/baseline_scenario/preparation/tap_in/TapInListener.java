package ch.ethz.matsim.baseline_scenario.preparation.tap_in;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.ethz.matsim.baseline_scenario.transit.events.PublicTransitEvent;

public class TapInListener implements GenericEventHandler, PersonDepartureEventHandler {
	private final TransitSchedule schedule;
	private final Population population;
	private final EventsManager eventsManager;

	private final Map<Id<Person>, Integer> lastEgressLDAs = new HashMap<>();

	public TapInListener(EventsManager eventsManager, TransitSchedule schedule, Population population) {
		this.schedule = schedule;
		this.population = population;
		this.eventsManager = eventsManager;
	}

	@Override
	public void reset(int iteration) {
		this.lastEgressLDAs.clear();
	}

	private void processTapIn(double time, Id<Person> personId, int lda) {
		eventsManager.processEvent(new TapInEvent(time, personId, lda));
	}

	public void handleEvent(PublicTransitEvent event) {
		Boolean hasSubscription = (Boolean) population.getPersons().get(event.getPersonId()).getAttributes()
				.getAttribute("ptSubscription");

		if (hasSubscription != null && hasSubscription) {
			Id<TransitStopFacility> currentAccessFacilityId = event.getAccessStopId();
			Integer currentAccessLDA = (Integer) schedule.getFacilities().get(currentAccessFacilityId).getAttributes()
					.getAttribute("LDA");

			if (currentAccessLDA != null) {
				// The current trip starts at a stop with LDA
				Integer lastEgressLDA = lastEgressLDAs.get(event.getPersonId());

				if (lastEgressLDA == null || !lastEgressLDA.equals(currentAccessLDA)) {
					// Either there is no previous LDA (= first leg), or there is an inter change
					// between LDA, both need a tap-in
					processTapIn(event.getTime(), event.getPersonId(), currentAccessLDA);
				}

				Integer currentEgressLDA = (Integer) schedule.getFacilities().get(event.getEgressStopId())
						.getAttributes().getAttribute("LDA");

				if (currentEgressLDA != null) {
					// Egress LDA exists, save it for later
					lastEgressLDAs.put(event.getPersonId(), currentEgressLDA);
				} else {
					// No new egress LDA, which means the next leg must be a tap-in
					lastEgressLDAs.remove(event.getPersonId());
				}
			} else {
				// The current trip starts at a stop without LDA
				lastEgressLDAs.remove(event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!event.getLegMode().equals("pt") && !event.getLegMode().equals("transit_walk")) {
			// Person departs with a different mode, reset the trip.
			lastEgressLDAs.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(GenericEvent event) {
		if (event instanceof PublicTransitEvent) {
			handleEvent((PublicTransitEvent) event);
		}
	}

}
