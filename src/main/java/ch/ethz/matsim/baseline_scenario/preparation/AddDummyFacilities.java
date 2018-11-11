package ch.ethz.matsim.baseline_scenario.preparation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

@Deprecated
public class AddDummyFacilities {
	private final Population population;
	private int currentIndex = 0;

	public AddDummyFacilities(Population population) {
		this.population = population;
	}

	public void run(ActivityFacilities facilities) {
		ActivityFacilitiesFactory factory = facilities.getFactory();

		for (Person person : population.getPersons().values()) {
			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = (Activity) element;

					if (activity.getFacilityId() == null) {
						Id<ActivityFacility> facilityId = Id.create(String.format("dummy_%d", currentIndex),
								ActivityFacility.class);
						activity.setFacilityId(facilityId);

						ActivityFacility facility = factory.createActivityFacility(facilityId,
								new Coord(activity.getCoord().getX(), activity.getCoord().getY()),
								activity.getLinkId());
						facilities.addActivityFacility(facility);

						ActivityOption option = factory.createActivityOption(activity.getType());
						facility.addActivityOption(option);

						currentIndex++;
					}
				}
			}
		}
	}
}
