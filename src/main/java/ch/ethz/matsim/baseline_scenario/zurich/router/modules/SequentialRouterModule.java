package ch.ethz.matsim.baseline_scenario.zurich.router.modules;

import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import ch.ethz.matsim.baseline_scenario.zurich.router.DefaultPlanRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.DefaultPopulationRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.PlanRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.PopulationRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.trip.TripRouter;

public class SequentialRouterModule extends AbstractModule {
	final private ActivityFacilities activityFacilities;

	public SequentialRouterModule(ActivityFacilities activityFacilities) {
		this.activityFacilities = activityFacilities;
	}

	@Override
	protected void configure() {
		// Needs StageActivityTypes
		// Needs MainModeIdentifier

		install(new BaseRouterModule());
	}

	@Provides
	public PopulationRouter provideSerialPopulationRouter(PlanRouter planRouter) {
		return new DefaultPopulationRouter(planRouter);
	}

	@Provides
	public PlanRouter providePlanRouter(TripRouter tripRouter, StageActivityTypes stageActivityTypes) {
		return new DefaultPlanRouter(tripRouter, stageActivityTypes, activityFacilities);
	}
}