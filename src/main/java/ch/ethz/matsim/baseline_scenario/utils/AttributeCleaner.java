package ch.ethz.matsim.baseline_scenario.utils;

import java.util.Collection;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class AttributeCleaner<T> {
	final private Collection<String> attributes;

	public AttributeCleaner(Collection<String> attributes) {
		this.attributes = attributes;
	}

	public ObjectAttributes run(Collection<? extends Identifiable<T>> items, ObjectAttributes source) {
		ObjectAttributes copy = new ObjectAttributes();

		for (Identifiable<T> item : items) {
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
