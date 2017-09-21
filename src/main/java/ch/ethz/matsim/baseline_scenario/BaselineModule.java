package ch.ethz.matsim.baseline_scenario;

import org.matsim.core.controler.AbstractModule;

import ch.ethz.matsim.baseline_scenario.scoring.BaselineScoringFunctionFactory;

public class BaselineModule extends AbstractModule {
	@Override
	public void install() {
		bindScoringFunctionFactory().to(BaselineScoringFunctionFactory.class).asEagerSingleton();
	}
}
