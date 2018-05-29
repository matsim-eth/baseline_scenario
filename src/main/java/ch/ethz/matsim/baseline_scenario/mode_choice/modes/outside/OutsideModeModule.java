package ch.ethz.matsim.baseline_scenario.mode_choice.modes.outside;

import ch.ethz.matsim.baseline_scenario.mode_choice.modes.AbstractModeModule;

public class OutsideModeModule extends AbstractModeModule {
	@Override
	public void installMode() {
		addModeAlternativeBinding("outside").toInstance(new OutsideAlternative());
	}
}
