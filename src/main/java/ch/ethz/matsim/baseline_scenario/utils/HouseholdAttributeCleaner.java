package ch.ethz.matsim.baseline_scenario.utils;

import java.util.Collection;

import org.matsim.households.Household;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class HouseholdAttributeCleaner {
	final private Collection<String> attributes;

	public HouseholdAttributeCleaner(Collection<String> attributes) {
		this.attributes = attributes;
	}

	public ObjectAttributes run(Collection<Household> items, ObjectAttributes source) {
		ObjectAttributes copy = new ObjectAttributes();

		for (Household item : items) {
			for (String attribute : attributes) {
				Object value = source.getAttribute(item.getId().toString(), attribute);

				if (value != null) {
					copy.putAttribute(item.getId().toString(), attribute, value);
				}
			}
		}

		return copy;
	}
}
