package ch.ethz.matsim.baseline_scenario.zurich.router.modules;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;

import ch.ethz.matsim.baseline_scenario.transit.connection.DefaultTransitConnectionFinder;
import ch.ethz.matsim.baseline_scenario.transit.connection.TransitConnectionFinder;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouter;
import ch.ethz.matsim.baseline_scenario.transit.routing.EnrichedTransitRouter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DefaultDepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.router.trip.PublicTransitTripRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.trip.TripRouter;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.LeastCostRaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.RaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorFactory;

public class PublicTransitRoutingModule extends AbstractModule {
	final private Optional<Network> network;
	final private Optional<TransitSchedule> transitSchedule;
	final private Optional<ModeRoutingParams> transitWalkParams;

	public PublicTransitRoutingModule(Network network, TransitSchedule transitSchedule,
			ModeRoutingParams transitWalkParams) {
		this.network = Optional.of(network);
		this.transitSchedule = Optional.of(transitSchedule);
		this.transitWalkParams = Optional.of(transitWalkParams);
	}

	public PublicTransitRoutingModule() {
		this.network = Optional.empty();
		this.transitSchedule = Optional.empty();
		this.transitWalkParams = Optional.empty();
	}

	@Override
	protected void configure() {
		MapBinder.newMapBinder(binder(), String.class, TripRouter.class).addBinding("pt")
				.to(PublicTransitTripRouter.class);

		if (network.isPresent()) {
			bind(Network.class).toInstance(network.get());
		}

		if (transitSchedule.isPresent()) {
			bind(TransitSchedule.class).toInstance(transitSchedule.get());
		}

		bind(TransitRouter.class).toProvider(SwissRailRaptorFactory.class);
	}
	
	@Provides
	@Singleton
	public RaptorIntermodalAccessEgress provideRaptorIntermodalAccessEgress() {
		return new DefaultRaptorIntermodalAccessEgress();
	}

	@Provides
	@Singleton
	public RaptorParametersForPerson provideRaptorParametersPerPerson(Config config) {
		return new DefaultRaptorParametersForPerson(config);
	}

	@Provides
	@Singleton
	public RaptorRouteSelector provideRaptorRouteSelector() {
		return new LeastCostRaptorRouteSelector();
	}

	@Provides
	@Singleton
	public Map<String, Provider<RoutingModule>> provideRaptorRoutingModules(
			@Named("transit_walk") ModeRoutingParams params, Population population) {
		return Collections.singletonMap("walk", new Provider<RoutingModule>() {
			@Override
			public RoutingModule get() {
				return new TeleportationRoutingModule("walk", population.getFactory(), params.getTeleportedModeSpeed(),
						params.getBeelineDistanceFactor());
			}
		});
	}

	@Provides
	@Singleton
	@Named("transit_walk")
	public ModeRoutingParams provideTransitWalkParams(Config config) {
		if (transitWalkParams.isPresent()) {
			return transitWalkParams.get();
		} else {
			return config.plansCalcRoute().getModeRoutingParams().get("walk");
		}
	}

	@Provides
	@Singleton
	public DepartureFinder provideDepartureFinder() {
		return new DefaultDepartureFinder();
	}

	@Provides
	@Singleton
	public TransitConnectionFinder provideTransitConnectionFinder(DepartureFinder departureFinder) {
		return new DefaultTransitConnectionFinder(departureFinder);
	}

	@Provides
	public EnrichedTransitRouter provideEnrichedTransitRouter(TransitRouter delegate,
			TransitConnectionFinder connectionFinder, PlansCalcRouteConfigGroup routeConfig,
			TransitRouterConfigGroup transitConfig, TransitSchedule transitSchedule, Network network) {
		double beelineDistanceFactor = routeConfig.getBeelineDistanceFactors().get("walk");
		double additionalTransferTime = transitConfig.getAdditionalTransferTime();

		return new DefaultEnrichedTransitRouter(delegate, transitSchedule, connectionFinder, network,
				beelineDistanceFactor, additionalTransferTime);
	}

	@Provides
	public PublicTransitTripRouter providePublicTransitTripRouter(DepartureFinder departureFinder,
			EnrichedTransitRouter transitRouter, TransitSchedule transitSchedule, Network network,
			@Named("transit_walk") ModeRoutingParams transitWalkParams) {
		return new PublicTransitTripRouter(network, transitSchedule, transitRouter, transitWalkParams);
	}
}
