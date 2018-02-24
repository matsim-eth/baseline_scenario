package ch.ethz.matsim.baseline_scenario.zurich.cutter.utils;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;

public class DefaultMergeOutsideActivities implements MergeOutsideActivities {
	@Override
	public void run(List<PlanElement> plan) {
		while (plan.size() > 1 && ((Activity) plan.get(0)).getType().equals("outside")
				&& ((Activity) plan.get(2)).getType().equals("outside")) {
			// While the first two activities are outside, remove the first one and the
			// following leg
			plan.remove(0);
			plan.remove(0);
		}

		while (plan.size() > 1 && ((Activity) plan.get(plan.size() - 3)).getType().equals("outside")
				&& ((Activity) plan.get(plan.size() - 1)).getType().equals("outside")) {
			// While the last two activities are outside, remove the last one and the
			// preceeding leg
			plan.remove(plan.size() - 1);
			plan.remove(plan.size() - 1);
		}

		for (int i = 0; i < plan.size() - 4; i += 2) {
			// As long as there can be found three following outside activities, remove the
			// middle one and the preceeding leg

			Activity firstActivity = (Activity) plan.get(i);
			Activity secondActivity = (Activity) plan.get(i + 2);
			Activity thirdActivity = (Activity) plan.get(i + 4);

			boolean firstIsOutside = firstActivity.getType().equals("outside");
			boolean secondIsOutside = secondActivity.getType().equals("outside");
			boolean thirdIsOutside = thirdActivity.getType().equals("outside");

			if (firstIsOutside && secondIsOutside && thirdIsOutside) {
				// We can delete the one in the middle
				plan.remove(i + 1);
				plan.remove(i + 1);

				// Check the current one again.
				i -= 2;

				firstActivity.setEndTime(secondActivity.getEndTime());
			}
		}

		if (plan.size() == 1) {
			Activity activity = (Activity) plan.get(0);

			if (activity.getType().equals("outside")) {
				plan.clear();
			}
		}
	}
}
