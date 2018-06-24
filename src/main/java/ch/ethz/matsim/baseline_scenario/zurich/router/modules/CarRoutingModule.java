package ch.ethz.matsim.baseline_scenario.zurich.router.modules;

import java.util.Optional;

import org.matsim.api.core.v01.network.Network;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import ch.ethz.matsim.baseline_scenario.zurich.router.trip.CarTripRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.trip.TripRouter;

public class CarRoutingModule extends AbstractModule {
	final private Optional<Network> roadNetwork;

	public CarRoutingModule(Network roadNetwork) {
		this.roadNetwork = Optional.of(roadNetwork);
	}

	public CarRoutingModule() {
		this.roadNetwork = Optional.empty();
	}

	@Override
	protected void configure() {
		if (roadNetwork.isPresent()) {
			bind(Key.get(Network.class, Names.named("road"))).toInstance(roadNetwork.get());
		}

		MapBinder.newMapBinder(binder(), String.class, TripRouter.class).addBinding("car").to(CarTripRouter.class);
	}

	@Provides
	public CarTripRouter provideCarTripRouter(@Named("road") Network roadNetwork) {
		return new CarTripRouter(roadNetwork);
	}
}
