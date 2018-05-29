package ch.ethz.matsim.baseline_scenario.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.pt.PtConstants;

import java.util.Arrays;

public class ASTRAScoringFunctionFactory implements ScoringFunctionFactory {
	final private PlanCalcScoreConfigGroup scoringConfig;
	final private ScenarioConfigGroup scenarioConfig;

	final private ActivityScoringByPersonAttributeBuilder activityByAttributeBuilder;
	final private ASTRAScoringParameters params;

	@Inject
	public ASTRAScoringFunctionFactory(PlanCalcScoreConfigGroup scoringConfig, ScenarioConfigGroup scenarioConfig,
                                       Population population, ASTRAScoringParameters params) {
		this.params = params;
		this.scoringConfig = scoringConfig;
		this.scenarioConfig = scenarioConfig;
		this.activityByAttributeBuilder = new ActivityScoringByPersonAttributeBuilder(population.getPersonAttributes(),
				Arrays.asList(PtConstants.TRANSIT_ACTIVITY_TYPE, "outside"));
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		ScoringParameters.Builder scoringBuilder = new ScoringParameters.Builder(scoringConfig,
				scoringConfig.getScoringParameters(null), scenarioConfig);
		activityByAttributeBuilder.apply(scoringBuilder, person);
		ScoringParameters parameters = scoringBuilder.build();

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		// sumScoringFunction.addScoringFunction(new
		// CharyparNagelActivityScoring(parameters));
		// sumScoringFunction.addScoringFunction(new ASTRALegScoring(params, params.averageIncome, priceCalculator));
		// sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(parameters,
		// network));
		// sumScoringFunction.addScoringFunction(new
		// CharyparNagelMoneyScoring(parameters));
		// sumScoringFunction.addScoringFunction(new
		// CharyparNagelAgentStuckScoring(parameters));
		// sumScoringFunction.addScoringFunction(new ASTRAStuckScoring(params));

		return sumScoringFunction;
	}
}
