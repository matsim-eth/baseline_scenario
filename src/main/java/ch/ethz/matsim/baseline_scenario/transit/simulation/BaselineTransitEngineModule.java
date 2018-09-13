package ch.ethz.matsim.baseline_scenario.transit.simulation;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;

public class BaselineTransitEngineModule extends AbstractQSimModule {
	public final static String BASELINE_TRANSIT_ENGINE_NAME = "BaselineTransitEngine";

	@Override
	protected void configureQSim() {
		bindDepartureHandler(BASELINE_TRANSIT_ENGINE_NAME).to(BaselineTransitEngine.class);
		bindMobsimEngine(BASELINE_TRANSIT_ENGINE_NAME).to(BaselineTransitEngine.class);
	}

	@Provides
	@Singleton
	public BaselineTransitEngine provideBaselineTransitEngine(EventsManager eventsManager,
			TransitSchedule transitSchedule, DepartureFinder departureFinder, QSim qsim) {
		return new BaselineTransitEngine(eventsManager, transitSchedule, departureFinder, qsim.getAgentCounter());
	}

	static public void configureComponents(QSimComponents components) {
		// Remove default transit engine
		components.activeMobsimEngines.remove(TransitEngineModule.TRANSIT_ENGINE_NAME);
		components.activeAgentSources.remove(TransitEngineModule.TRANSIT_ENGINE_NAME);
		components.activeDepartureHandlers.remove(TransitEngineModule.TRANSIT_ENGINE_NAME);

		// Add baseline engine
		components.activeMobsimEngines.add(BASELINE_TRANSIT_ENGINE_NAME);
		components.activeDepartureHandlers.add(BASELINE_TRANSIT_ENGINE_NAME);
	}
}
