package ch.ethz.matsim.baseline_scenario.transit;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.baseline_scenario.transit.connection.DefaultTransitConnectionFinder;
import ch.ethz.matsim.baseline_scenario.transit.connection.TransitConnectionFinder;
import ch.ethz.matsim.baseline_scenario.transit.routing.BaselineTransitRoutingModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouter;
import ch.ethz.matsim.baseline_scenario.transit.routing.EnrichedTransitRouter;
import ch.ethz.matsim.baseline_scenario.transit.simulation.BaselineTransitQSimModule;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DefaultDepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorFactory;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class BaselineTransitModule extends AbstractModule {
	@Override
	public void install() {
		installQSimModule(new BaselineTransitQSimModule());
		bind(TransitRouter.class).toProvider(SwissRailRaptorFactory.class);
		addRoutingModuleBinding("pt").to(BaselineTransitRoutingModule.class);
	}

	@Provides
	public EnrichedTransitRouter provideEnrichedTransitRouter(TransitRouter delegate, TransitSchedule transitSchedule,
			TransitConnectionFinder connectionFinder, Network network, PlansCalcRouteConfigGroup routeConfig,
			TransitRouterConfigGroup transitConfig) {
		double beelineDistanceFactor = routeConfig.getBeelineDistanceFactors().get("walk");
		double additionalTransferTime = transitConfig.getAdditionalTransferTime();

		return new DefaultEnrichedTransitRouter(delegate, transitSchedule, connectionFinder, network,
				beelineDistanceFactor, additionalTransferTime);
	}

	@Provides
	public BaselineTransitRoutingModule provideBaselineTransitRoutingModule(EnrichedTransitRouter transitRouter,
			TransitSchedule transitSchedule) {
		return new BaselineTransitRoutingModule(transitRouter, transitSchedule);
	}

	@Provides
	@Singleton
	public TransitSchedule provideTransitSchedule(Scenario scenario) {
		return scenario.getTransitSchedule();
	}

	@Provides
	@Singleton
	public TransitConnectionFinder provideTransitConnectionFinder(DepartureFinder departureFinder) {
		return new DefaultTransitConnectionFinder(departureFinder);
	}

	@Provides
	@Singleton
	public DepartureFinder provideDepartureFinder() {
		return new DefaultDepartureFinder();
	}
}
