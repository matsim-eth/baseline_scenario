package ch.ethz.matsim.baseline_scenario.utils;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class ShiftTimes {
	final private double interval;
	final private Random random;
	final private boolean shiftPerPlan;

	public ShiftTimes(double interval, Random random, boolean shiftPerPlan) {
		this.interval = interval;
		this.random = random;
		this.shiftPerPlan = shiftPerPlan;
	}

	public void apply(Population population) {
		for (Person person : population.getPersons().values()) {
			double r = random.nextDouble();

			for (Plan plan : person.getPlans()) {
				if (shiftPerPlan) {
					r = random.nextDouble();
				}

				List<Activity> activities = person.getSelectedPlan().getPlanElements().stream()
						.filter(p -> p instanceof Activity).map(Activity.class::cast).collect(Collectors.toList());

				double firstEndTime = activities.get(0).getEndTime();
				double minimumOffset = -firstEndTime;
				double offset = Math.max(minimumOffset, r * interval - 0.5 * interval);

				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						Activity activity = (Activity) planElement;
						activity.setEndTime(activity.getEndTime() + offset);
						activity.setStartTime(activity.getStartTime() + offset);
					}
				}
			}
		}
	}

	static public void main(String args[]) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(args[0]);

		double interval = args.length > 2 ? Double.parseDouble(args[2]) : 1200.0;
		new ShiftTimes(interval, new Random(), false).apply(scenario.getPopulation());

		new PopulationWriter(scenario.getPopulation()).write(args[1]);
	}
}