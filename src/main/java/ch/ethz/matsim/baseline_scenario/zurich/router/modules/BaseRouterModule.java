package ch.ethz.matsim.baseline_scenario.zurich.router.modules;

import java.util.Map;

import org.matsim.core.router.MainModeIdentifier;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import ch.ethz.matsim.baseline_scenario.zurich.router.trip.ModeAwareTripRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.trip.TripRouter;

public class BaseRouterModule extends AbstractModule {
	@Override
	protected void configure() {
		// Needs MainModeIdentifier
	}

	@Provides
	public TripRouter provideTripRouter(MainModeIdentifier mainModeIdentifier, Map<String, TripRouter> tripRouters) {
		return new ModeAwareTripRouter(tripRouters, mainModeIdentifier);
	}
}