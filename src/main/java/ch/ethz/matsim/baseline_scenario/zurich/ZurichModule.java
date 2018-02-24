package ch.ethz.matsim.baseline_scenario.zurich;

import java.util.Arrays;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.PtConstants;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.baseline_scenario.scoring.ActivityScoringByPersonAttributeBuilder;

public class ZurichModule extends AbstractModule {
	@Override
	public void install() {
	}

	@Provides
	@Singleton
	public ActivityScoringByPersonAttributeBuilder provideActivityScoringByPersonAttributeBuilder(
			Population population) {
		return new ActivityScoringByPersonAttributeBuilder(population.getPersonAttributes(),
				Arrays.asList(PtConstants.TRANSIT_ACTIVITY_TYPE, "outside"));
	}
}
