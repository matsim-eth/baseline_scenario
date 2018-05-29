package ch.ethz.matsim.baseline_scenario.mode_choice.modes.walk;

import ch.ethz.matsim.baseline_scenario.mode_choice.modes.AbstractModeModule;
import ch.ethz.matsim.baseline_scenario.scoring.ASTRAScoringParameters;
import ch.ethz.matsim.mode_choice.mnl.ModeChoiceAlternative;
import ch.ethz.matsim.mode_choice.mnl.prediction.*;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;

public class WalkModeModule extends AbstractModeModule {
	final private static String MODE = "walk";

	@Override
	public void installMode() {
		addControlerListenerBinding().to(Key.get(PredictionCacheCleaner.class, Names.named(MODE)));
		addModeAlternativeBinding(MODE).to(Key.get(ModeChoiceAlternative.class, Names.named(MODE)));
	}

	@Singleton
	@Provides
	@Named(MODE)
	public ModeChoiceAlternative provideModeChoiceAlternative(ASTRAScoringParameters parameters,
                                                              PlansCalcRouteConfigGroup routeConfig, @Named(MODE) PredictionCache cache) {
		return new WalkAlternative(parameters, new FixedSpeedPredictor(routeConfig.getTeleportedModeSpeeds().get(MODE),
				new CrowflyDistancePredictor(routeConfig.getBeelineDistanceFactors().get(MODE))), cache);
	}

	@Singleton
	@Provides
	@Named(MODE)
	public PredictionCache providePredictionCache() {
		return new HashPredictionCache();
	}

	@Singleton
	@Provides
	@Named(MODE)
	public PredictionCacheCleaner providePredictionCacheCleaner(@Named(MODE) PredictionCache cache) {
		return new PredictionCacheCleaner(cache);
	}
}
