package ch.ethz.matsim.baseline_scenario.transit.simulation;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;

public class BaselineTransitQSimModule extends AbstractQSimModule {
	public static final String COMPONENT_NAME = "BaselineTransit";

	@Override
	protected void configureQSim() {
		addNamedComponent(BaselineTransitEngine.class, COMPONENT_NAME);
	}

	@Provides
	@Singleton
	public BaselineTransitEngine provideBaselineTransitEngine(EventsManager eventsManager,
			TransitSchedule transitSchedule, DepartureFinder departureFinder, QSim qsim) {
		return new BaselineTransitEngine(eventsManager, transitSchedule, departureFinder, qsim.getAgentCounter());
	}
	
	static public void configureComponents(QSimComponentsConfig components) {
		components.removeNamedComponent(TransitEngineModule.TRANSIT_ENGINE_NAME);
		components.addNamedComponent(BaselineTransitQSimModule.COMPONENT_NAME);
	}
}
