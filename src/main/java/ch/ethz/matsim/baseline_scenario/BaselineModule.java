package ch.ethz.matsim.baseline_scenario;

import org.matsim.core.controler.AbstractModule;

import ch.ethz.matsim.baseline_scenario.scoring.BaselineScoringFunctionFactory;
import ch.sbb.matsim.mobsim.qsim.SBBQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class BaselineModule extends AbstractModule {
	@Override
	public void install() {
		bindScoringFunctionFactory().to(BaselineScoringFunctionFactory.class).asEagerSingleton();
		install(new SBBQSimModule());
		install(new SwissRailRaptorModule());
	}
}
