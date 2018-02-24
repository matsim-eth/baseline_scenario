package ch.ethz.matsim.baseline_scenario.zurich.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class OutsideAttributeSetter {
	final private Network network;

	public OutsideAttributeSetter(Network network) {
		this.network = network;
	}

	private boolean isOutside(Id<Link> linkId) {
		if (linkId.toString().contains("outside")) {
			return true;
		}

		Link link = network.getLinks().get(linkId);

		if (link == null) {
			return false;
		}

		return link.getFromNode().getId().toString().contains("outside")
				|| link.getToNode().getId().toString().contains("outside");
	}

	private boolean isOutside(Activity activity) {
		return activity.getType().contains("outside") || isOutside(activity.getLinkId());
	}

	private boolean isOutside(Leg leg) {
		return leg.getMode().contains("outside") || isOutside(leg.getRoute().getStartLinkId())
				|| isOutside(leg.getRoute().getEndLinkId());
	}

	public void run(Population population) {
		for (Person person : population.getPersons().values()) {
			boolean isOutside = false;

			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						if (isOutside((Activity) element)) {
							isOutside = true;
						}
					} else {
						if (isOutside((Leg) element)) {
							isOutside = true;
						}
					}
				}
			}

			person.getAttributes().putAttribute("outside", isOutside);
		}
	}
}
