package ch.ethz.matsim.baseline_scenario.transit;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.Transit;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorFactory;

public class BaselineTransitModule extends AbstractModule {
	@Override
	public void install() {
		bind(TransitRouter.class).toProvider(SwissRailRaptorFactory.class);

		addRoutingModuleBinding("pt").toProvider(Transit.class);
		addRoutingModuleBinding(TransportMode.transit_walk)
				.to(Key.get(RoutingModule.class, Names.named(TransportMode.walk)));
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
}
