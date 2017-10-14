package ch.ethz.matsim.baseline_scenario.scoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class ActivityScoringByPersonAttributeBuilder {	
	final private ObjectAttributes attributes;
	
	public ActivityScoringByPersonAttributeBuilder(ObjectAttributes attributes) {
		this.attributes = attributes;
	}
	
	public void apply(ScoringParameters.Builder scoringBuilder, Person person) {
		for (Plan plan : person.getPlans()) {
			for (PlanElement element : plan.getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = (Activity) element;
					
					ActivityUtilityParameters.Builder activityBuilder = new ActivityUtilityParameters.Builder();
					
					if (!activity.getType().contains("interaction")) {					
						activityBuilder.setTypicalDuration_s((Double) attributes.getAttribute(person.getId().toString(), "typicalDuration_" + activity.getType()));
						activityBuilder.setMinimalDuration((Double) attributes.getAttribute(person.getId().toString(), "minimalDuration_" + activity.getType()));
						activityBuilder.setEarliestEndTime((Double) attributes.getAttribute(person.getId().toString(), "earliestEndTime_" + activity.getType()));
						activityBuilder.setLatestStartTime((Double) attributes.getAttribute(person.getId().toString(), "latestStartTime_" + activity.getType()));
						activityBuilder.setZeroUtilityComputation(new ActivityUtilityParameters.SameAbsoluteScore());					
					} else {
						// Do NOT score interaction activities
						activityBuilder.setScoreAtAll(false);
						activityBuilder.setZeroUtilityComputation(new ActivityUtilityParameters.SameAbsoluteScore());
					}
					
					scoringBuilder.setActivityParameters(activity.getType(), activityBuilder);
				}
			}
		}
	}
}
