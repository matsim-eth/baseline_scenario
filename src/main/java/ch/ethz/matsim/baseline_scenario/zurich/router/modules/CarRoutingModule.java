package ch.ethz.matsim.baseline_scenario.zurich.router.modules;

import org.matsim.api.core.v01.network.Network;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;

import ch.ethz.matsim.baseline_scenario.zurich.router.trip.CarTripRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.trip.TripRouter;

public class CarRoutingModule extends AbstractModule {
	final private Network roadNetwork;

	public CarRoutingModule(Network roadNetwork) {
		this.roadNetwork = roadNetwork;
	}

	@Override
	protected void configure() {
		MapBinder.newMapBinder(binder(), String.class, TripRouter.class).addBinding("car").to(CarTripRouter.class);
	}

	@Provides
	public CarTripRouter provideCarTripRouter() {
		return new CarTripRouter(roadNetwork);
	}
}
