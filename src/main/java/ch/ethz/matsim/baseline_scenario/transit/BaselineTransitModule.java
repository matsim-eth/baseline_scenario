package ch.ethz.matsim.baseline_scenario.transit;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
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
import ch.ethz.matsim.baseline_scenario.transit.simulation.BaselineTransitPlugin;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DefaultDepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorFactory;

public class BaselineTransitModule extends AbstractModule {
	@Override
	public void install() {
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
	public Collection<AbstractQSimPlugin> provideQSimPlugins(Config config) {
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();

		plugins.add(new MessageQueuePlugin(config));
		plugins.add(new ActivityEnginePlugin(config));
		plugins.add(new QNetsimEnginePlugin(config));

		if (config.network().isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin(config));
		}

		plugins.add(new BaselineTransitPlugin(config));
		plugins.add(new TeleportationPlugin(config));
		plugins.add(new PopulationPlugin(config));

		return plugins;
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
