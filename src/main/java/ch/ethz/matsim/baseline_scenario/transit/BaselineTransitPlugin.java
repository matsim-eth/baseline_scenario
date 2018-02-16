package ch.ethz.matsim.baseline_scenario.transit;

import java.util.Collection;
import java.util.Collections;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class BaselineTransitPlugin extends AbstractQSimPlugin {
	public BaselineTransitPlugin(Config config) {
		super(config);
	}

	@Override
	public Collection<? extends Module> modules() {
		return Collections.singletonList(new AbstractModule() {
			@Override
			protected void configure() {
			}

			@Provides
			@Singleton
			public BaselineTransitEngine provideBaselineTransitEngine(EventsManager eventsManager,
					TransitSchedule transitSchedule, Network network) {
				return new BaselineTransitEngine(eventsManager, transitSchedule, network);
			}
		});
	}

	@Override
	public Collection<Class<? extends DepartureHandler>> departureHandlers() {
		return Collections.singletonList(BaselineTransitEngine.class);
	}

	@Override
	public Collection<Class<? extends MobsimEngine>> engines() {
		return Collections.singletonList(BaselineTransitEngine.class);
	}
}
