package ch.ethz.matsim.baseline_scenario.mode_choice.modes.outside;

import ch.ethz.matsim.mode_choice.ModeChoiceTrip;
import ch.ethz.matsim.mode_choice.mnl.ModeChoiceAlternative;

public class OutsideAlternative implements ModeChoiceAlternative {
	@Override
	public double estimateUtility(ModeChoiceTrip trip) {
		return 0.0;
	}
	
	@Override
	public boolean isFeasible(ModeChoiceTrip trip) {
		return true;
	}

	@Override
	public boolean isChainMode() {
		return false;
	}
}
