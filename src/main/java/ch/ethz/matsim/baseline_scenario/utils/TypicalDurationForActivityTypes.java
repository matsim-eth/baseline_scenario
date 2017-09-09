package ch.ethz.matsim.baseline_scenario.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class TypicalDurationForActivityTypes {
	private void applyPerson(ObjectAttributes attributes, Id<Person> personId, String activityType,
			double typicalDuration) {
		attributes.putAttribute(personId.toString(), "typicalDuration_" + activityType, typicalDuration);
		attributes.putAttribute(personId.toString(), "minimalDuration_" + activityType, 0.0);
		attributes.putAttribute(personId.toString(), "earliestEndTime_" + activityType, 0.0 * 3600.0);
		attributes.putAttribute(personId.toString(), "latestStartTime_" + activityType, 24.0 * 3600.0);
	}

	public void run(Population population, ActivityFacilities facilities) {
		Map<String, Integer> maximumCounts = new HashMap<>();

		for (Person person : population.getPersons().values()) {
			if (person.getPlans().size() > 1)
				throw new IllegalStateException();

			Plan plan = person.getSelectedPlan();
			List<Activity> activities = plan.getPlanElements().stream().filter(Activity.class::isInstance)
					.map(Activity.class::cast).collect(Collectors.toList());

			Map<String, AtomicInteger> typeCounts = new HashMap<>();
			boolean firstAndLastIsSameType = activities.get(0).getType()
					.equals(activities.get(activities.size() - 1).getType());

			if (activities.size() == 1) {
				Activity current = activities.get(0);

				typeCounts.put(current.getType(), new AtomicInteger(1));

				applyPerson(population.getPersonAttributes(), person.getId(), current.getType() + "_" + 1,
						24.0 * 3600.0);
				activities.get(0).setType(activities.get(0).getType() + "_" + 1);
			} else {
				for (int index = 0; index < activities.size(); index++) {
					Activity current = activities.get(index);

					if (current.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
						throw new IllegalArgumentException();
					}

					if (!typeCounts.containsKey(current.getType())) {
						typeCounts.put(current.getType(), new AtomicInteger(0));
					}

					int currentCount = typeCounts.get(current.getType()).incrementAndGet();
					String countedType = current.getType() + "_" + currentCount;

					boolean isFirstActivity = index == 0;
					boolean isLastActivity = index == activities.size() - 1;

					if (isFirstActivity) {
						if (!firstAndLastIsSameType) {
							applyPerson(population.getPersonAttributes(), person.getId(), countedType,
									current.getEndTime());
							current.setType(countedType);
						}

						current.setType(countedType);
					} else if (isLastActivity) {
						if (!firstAndLastIsSameType) {
							double duration = 24.0 * 3600.0 - current.getStartTime();

							if (duration <= 0.0) {
								throw new IllegalStateException(person.getId().toString());
							}

							applyPerson(population.getPersonAttributes(), person.getId(), countedType, duration);
							current.setType(countedType);
						} else {
							double duration = activities.get(0).getEndTime() + 24.0 * 3600.0 - current.getStartTime();

							if (duration <= 0.0) {
								throw new IllegalStateException(person.getId().toString());
							}

							applyPerson(population.getPersonAttributes(), person.getId(), current.getType() + "_" + 1,
									duration);
							current.setType(current.getType() + "_" + 1);
						}
					} else {
						double duration = current.getEndTime() - current.getStartTime();

						if (duration <= 0.0) {
							throw new IllegalStateException(person.getId().toString());
						}

						applyPerson(population.getPersonAttributes(), person.getId(), countedType, duration);
						current.setType(countedType);
					}
				}
			}
			
			for (Map.Entry<String, AtomicInteger> entry : typeCounts.entrySet()) {
				maximumCounts.put(entry.getKey(), Math.max(maximumCounts.getOrDefault(entry.getKey(), 0), entry.getValue().get()));
			}
		}
		
		for (Map.Entry<String, Integer> entry : maximumCounts.entrySet()) {
			for (ActivityFacility facility : facilities.getFacilitiesForActivityType(entry.getKey()).values()) {
				ActivityOption original = facility.getActivityOptions().get(entry.getKey());
				
				for (int i = 1; i <= entry.getValue(); i++) {
					ActivityOption option = new ActivityOptionImpl(entry.getKey() + "_" + i);
					
					option.setCapacity(original.getCapacity());
					original.getOpeningTimes().forEach(o -> option.addOpeningTime(o));
					
					facility.addActivityOption(option);
				}
			}
		}
	}
}
