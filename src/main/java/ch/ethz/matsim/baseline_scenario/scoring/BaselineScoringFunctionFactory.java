package ch.ethz.matsim.baseline_scenario.scoring;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
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
	final private ActivityScoringBuilder activityScoringBuilder;

	@Inject
	public BaselineScoringFunctionFactory(PlanCalcScoreConfigGroup scoringConfig, ScenarioConfigGroup scenarioConfig,
			Network network, Population population, ActivityScoringBuilder activityScoringBuilder) {
		this.scoringConfig = scoringConfig;
		this.scenarioConfig = scenarioConfig;
		this.network = network;
		this.activityScoringBuilder = activityScoringBuilder;
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		ScoringParameters.Builder scoringBuilder = new ScoringParameters.Builder(scoringConfig,
				scoringConfig.getScoringParameters(null), scenarioConfig);
		activityScoringBuilder.apply(scoringBuilder, person);
		ScoringParameters parameters = scoringBuilder.build();

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(parameters, network));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(parameters));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(parameters));

		return sumScoringFunction;
	}
}
