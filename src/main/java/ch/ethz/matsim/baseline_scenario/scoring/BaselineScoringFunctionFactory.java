package ch.ethz.matsim.baseline_scenario.scoring;

import java.util.Arrays;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.ActivityUtilityParameters.ZeroUtilityComputation;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

import com.google.inject.Inject;

public class BaselineScoringFunctionFactory implements ScoringFunctionFactory {
	final private PlanCalcScoreConfigGroup scoringConfig;
	final private ScenarioConfigGroup scenarioConfig;
	
	final private Network network;
	final private Population population;
	
	@Inject
	public BaselineScoringFunctionFactory(PlanCalcScoreConfigGroup scoringConfig, ScenarioConfigGroup scenarioConfig, Network network, Population population) {
		this.scoringConfig = scoringConfig;
		this.scenarioConfig = scenarioConfig;
		this.network = network;
		this.population = population;
	}
	
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		ScoringParameters.Builder scoringBuilder = new ScoringParameters.Builder(scoringConfig, scoringConfig.getScoringParameters(null), scenarioConfig);
		
		for (Plan plan : person.getPlans()) {
			for (PlanElement element : plan.getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = (Activity) element;
					
					ActivityUtilityParameters.Builder activityBuilder = new ActivityUtilityParameters.Builder();
					activityBuilder.setTypicalDuration_s((Double) population.getPersonAttributes().getAttribute(person.getId().toString(), "typicalDuration_" + activity.getType()));
					activityBuilder.setMinimalDuration((Double) population.getPersonAttributes().getAttribute(person.getId().toString(), "minimalDuration_" + activity.getType()));
					activityBuilder.setEarliestEndTime((Double) population.getPersonAttributes().getAttribute(person.getId().toString(), "earliestEndTime_" + activity.getType()));
					activityBuilder.setLatestStartTime((Double) population.getPersonAttributes().getAttribute(person.getId().toString(), "latestStartTime_" + activity.getType()));
					activityBuilder.setZeroUtilityComputation(new ActivityUtilityParameters.SameAbsoluteScore());					
					
					scoringBuilder.setActivityParameters(activity.getType(), activityBuilder);
				}
			}
		}
		
		ScoringParameters parameters = scoringBuilder.build();

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(parameters, network));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(parameters));
		
		return sumScoringFunction;
	}
}
