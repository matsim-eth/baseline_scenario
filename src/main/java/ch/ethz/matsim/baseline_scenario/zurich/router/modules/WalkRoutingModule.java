package ch.ethz.matsim.baseline_scenario.zurich.router.modules;

import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import ch.ethz.matsim.baseline_scenario.zurich.router.trip.TeleportationTripRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.trip.TripRouter;

public class WalkRoutingModule extends AbstractModule {
	final private ModeRoutingParams modeParams;

	public WalkRoutingModule(ModeRoutingParams modeParams) {
		this.modeParams = modeParams;
	}

	@Override
	protected void configure() {
		MapBinder.newMapBinder(binder(), String.class, TripRouter.class).addBinding("walk")
				.to(Key.get(TeleportationTripRouter.class, Names.named("walk")));
	}

	@Provides
	@Named("walk")
	public TeleportationTripRouter provideWalkTripRouter() {
		return new TeleportationTripRouter(modeParams);
	}
}
