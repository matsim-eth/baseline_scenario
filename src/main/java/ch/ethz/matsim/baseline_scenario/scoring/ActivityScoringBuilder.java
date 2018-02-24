package ch.ethz.matsim.baseline_scenario.scoring;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.functions.ScoringParameters;

public interface ActivityScoringBuilder {
	void apply(ScoringParameters.Builder scoringBuilder, Person person);
}
