package ch.ethz.matsim.baseline_scenario.utils;

import java.util.Collection;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.Household;
import org.matsim.households.Households;

public class HouseholdsCleaner {
	final private Collection<Id<Person>> personsIds;

	public HouseholdsCleaner(Collection<Id<Person>> personsIds) {
		this.personsIds = personsIds;
	}

	public void run(Households households) {
		Iterator<Household> iterator = households.getHouseholds().values().iterator();

		while (iterator.hasNext()) {
			Household household = iterator.next();
			Iterator<Id<Person>> memberIterator = household.getMemberIds().iterator();

			while (memberIterator.hasNext()) {
				Id<Person> memberId = memberIterator.next();

				if (!personsIds.contains(memberId)) {
					memberIterator.remove();
				}
			}

			if (household.getMemberIds().size() == 0) {
				iterator.remove();
			}
		}
	}
}
