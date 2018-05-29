package ch.ethz.matsim.baseline_scenario.mode_choice.modes.public_transit;

import ch.ethz.matsim.baseline_scenario.mode_choice.modes.AbstractModeModule;
import ch.ethz.matsim.baseline_scenario.scoring.ASTRAScoringParameters;
import ch.ethz.matsim.baseline_scenario.transit.routing.EnrichedTransitRouter;
import ch.ethz.matsim.mode_choice.mnl.ModeChoiceAlternative;
import ch.ethz.matsim.mode_choice.mnl.prediction.HashPredictionCache;
import ch.ethz.matsim.mode_choice.mnl.prediction.PredictionCache;
import ch.ethz.matsim.mode_choice.mnl.prediction.PredictionCacheCleaner;
import ch.ethz.matsim.baseline_scenario.transit.routing.QueueBasedThreadSafeEnrichedTransitRouter;
import ch.ethz.matsim.baseline_scenario.transit.routing.ThreadSafeEnrichedTransitRouter;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;

public class PublicTransitModeModule extends AbstractModeModule {
	final private static String MODE = "pt";

	@Override
	public void installMode() {
		addControlerListenerBinding().to(Key.get(PredictionCacheCleaner.class, Names.named(MODE)));
		addModeAlternativeBinding(MODE).to(Key.get(ModeChoiceAlternative.class, Names.named(MODE)));
	}

	@Singleton
	@Provides
	public ThreadSafeEnrichedTransitRouter provideThreadSafeTransitRouter(GlobalConfigGroup globalConfig,
			Provider<EnrichedTransitRouter> transitRouter) {
		int numberOfInstances = globalConfig.getNumberOfThreads();
		return new QueueBasedThreadSafeEnrichedTransitRouter(numberOfInstances, transitRouter);
	}

	@Singleton
	@Provides
	@Named(MODE)
	public ModeChoiceAlternative provideModeChoiceAlternative(ASTRAScoringParameters parameters,
                                                              ThreadSafeEnrichedTransitRouter transitRouter, @Named(MODE) PredictionCache cache, Population population) {
		return new PublicTransportAlternative(parameters, new ASTRAPublicTransitTripPredictor(transitRouter), cache,
				population.getPersonAttributes());
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
