package ch.ethz.matsim.baseline_scenario.zurich.router.modules;

import java.util.LinkedList;
import java.util.List;

import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;

import ch.ethz.matsim.baseline_scenario.zurich.router.parallel.DefaultParallelPlanRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.parallel.DefaultParallelPopulationRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.parallel.DefaultParallelTripRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.parallel.ParallelPlanRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.parallel.ParallelPopulationRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.parallel.ParallelTripRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.trip.TripRouter;

public class ParallelRouterModule extends AbstractModule {
	final private ActivityFacilities activityFacilities;
	final private int numberOfTripRunners;

	public ParallelRouterModule(int numberOfTripRunners, ActivityFacilities activityFacilities) {
		this.activityFacilities = activityFacilities;
		this.numberOfTripRunners = numberOfTripRunners;
	}

	@Override
	protected void configure() {
		// Needs StageActivityTypes
		// Needs MainModeIdentifier

		install(new BaseRouterModule());
	}

	@Provides
	public ParallelPopulationRouter provideParallelPopulationRouter(ParallelPlanRouter planRouter) {
		return new DefaultParallelPopulationRouter(planRouter);
	}

	@Provides
	public ParallelPlanRouter provideParallelPlanRouter(ParallelTripRouter tripRouter,
			StageActivityTypes stageActivityTypes) {
		return new DefaultParallelPlanRouter(tripRouter, stageActivityTypes, activityFacilities);
	}

	@Provides
	public ParallelTripRouter provideParallelTripRouter(Provider<TripRouter> tripRouterProvider) {
		List<TripRouter> instances = new LinkedList<>();

		for (int i = 0; i < numberOfTripRunners; i++) {
			instances.add(tripRouterProvider.get());
		}

		return new DefaultParallelTripRouter(instances);
	}
}
