package ch.ethz.matsim.baseline_scenario.zurich.router.modules;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;

import ch.ethz.matsim.baseline_scenario.transit.connection.DefaultTransitConnectionFinder;
import ch.ethz.matsim.baseline_scenario.transit.connection.TransitConnectionFinder;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouter;
import ch.ethz.matsim.baseline_scenario.transit.routing.EnrichedTransitRouter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DefaultDepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.router.trip.PublicTransitTripRouter;
import ch.ethz.matsim.baseline_scenario.zurich.router.trip.TripRouter;

public class PublicTransitRoutingModule extends AbstractModule {
	final private Network network;
	final private TransitSchedule transitSchedule;
	final private ModeRoutingParams transitWalkParams;

	public PublicTransitRoutingModule(Network network, TransitSchedule transitSchedule,
			ModeRoutingParams transitWalkParams) {
		this.network = network;
		this.transitSchedule = transitSchedule;
		this.transitWalkParams = transitWalkParams;
	}

	@Override
	protected void configure() {
		MapBinder.newMapBinder(binder(), String.class, TripRouter.class).addBinding("pt")
				.to(PublicTransitTripRouter.class);
		bind(TransitSchedule.class).toInstance(transitSchedule);
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
			TransitRouterConfigGroup transitConfig) {
		double beelineDistanceFactor = routeConfig.getBeelineDistanceFactors().get("walk");
		double additionalTransferTime = transitConfig.getAdditionalTransferTime();

		return new DefaultEnrichedTransitRouter(delegate, transitSchedule, connectionFinder, network,
				beelineDistanceFactor, additionalTransferTime);
	}

	@Provides
	public PublicTransitTripRouter providePublicTransitTripRouter(DepartureFinder departureFinder,
			EnrichedTransitRouter transitRouter) {
		return new PublicTransitTripRouter(network, transitSchedule, transitRouter, transitWalkParams);
	}
}
