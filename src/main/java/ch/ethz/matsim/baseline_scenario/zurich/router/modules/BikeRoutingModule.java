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

public class BikeRoutingModule extends AbstractModule {
	final private ModeRoutingParams modeParams;

	public BikeRoutingModule(ModeRoutingParams modeParams) {
		this.modeParams = modeParams;
	}

	@Override
	protected void configure() {
		MapBinder.newMapBinder(binder(), String.class, TripRouter.class).addBinding("bike")
				.to(Key.get(TeleportationTripRouter.class, Names.named("bike")));
	}

	@Provides
	@Named("bike")
	public TeleportationTripRouter provideBikeTripRouter() {
		return new TeleportationTripRouter(modeParams);
	}
}