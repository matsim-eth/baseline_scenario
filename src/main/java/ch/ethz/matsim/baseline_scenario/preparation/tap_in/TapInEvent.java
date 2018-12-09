package ch.ethz.matsim.baseline_scenario.preparation.tap_in;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

public class TapInEvent extends GenericEvent implements HasPersonId {
	public static final String NAME = "tap in";

	private final Id<Person> personId;
	private final int lda;

	public TapInEvent(double time, Id<Person> personId, int lda) {
		super(NAME, time);
		this.personId = personId;
		this.lda = lda;
	}

	@Override
	public Id<Person> getPersonId() {
		return personId;
	}

	public int getLDA() {
		return lda;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put("person", personId.toString());
		attributes.put("lda", String.valueOf(lda));
		return attributes;
	}
}
