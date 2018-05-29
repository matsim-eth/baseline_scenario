package ch.ethz.matsim.baseline_scenario.mode_choice.modes.car;

import ch.ethz.matsim.baseline_scenario.mode_choice.modes.AbstractModeModule;
import ch.ethz.matsim.baseline_scenario.scoring.ASTRAScoringParameters;
import ch.ethz.matsim.mode_choice.mnl.ModeChoiceAlternative;
import ch.ethz.matsim.mode_choice.mnl.prediction.HashPredictionCache;
import ch.ethz.matsim.mode_choice.mnl.prediction.NetworkPathPredictor;
import ch.ethz.matsim.mode_choice.mnl.prediction.PredictionCache;
import ch.ethz.matsim.mode_choice.mnl.prediction.PredictionCacheCleaner;
import ch.ethz.matsim.mode_choice.utils.QueueBasedThreadSafeDijkstra;
import ch.ethz.matsim.mode_choice.utils.ThreadSafeLeastCostPathCalculator;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.Collections;

public class CarModeModule extends AbstractModeModule {
	final private static String MODE = "car";

	@Override
	public void installMode() {
		addControlerListenerBinding().to(Key.get(PredictionCacheCleaner.class, Names.named(MODE)));
		addModeAlternativeBinding(MODE).to(Key.get(ModeChoiceAlternative.class, Names.named(MODE)));
	}

	@Singleton
	@Provides
	@Named(MODE)
	public ModeChoiceAlternative provideModeChoiceAlternative(ASTRAScoringParameters parameters,
                                                              ThreadSafeLeastCostPathCalculator router, @Named(MODE) PredictionCache cache) {
		return new CarAlternative(parameters, new NetworkPathPredictor(router), cache);
	}

	@Singleton
	@Provides
	@Named("road")
	public Network provideRoadNetwork(Network network) {
		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(roadNetwork, Collections.singleton("car"));
		return roadNetwork;
	}

	@Singleton
	@Provides
	public ThreadSafeLeastCostPathCalculator provideQueueBasedLeastCostPathCalculator(GlobalConfigGroup globalConfig,
                                                                                      @Named("road") Network network, @Named("car") TravelTime travelTime) {
		int numberOfInstances = globalConfig.getNumberOfThreads();
		return new QueueBasedThreadSafeDijkstra(numberOfInstances, network,
				new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
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
