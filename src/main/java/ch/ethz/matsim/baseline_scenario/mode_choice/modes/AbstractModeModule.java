package ch.ethz.matsim.baseline_scenario.mode_choice.modes;

import ch.ethz.matsim.mode_choice.mnl.ModeChoiceAlternative;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import org.matsim.core.controler.AbstractModule;

public abstract class AbstractModeModule extends AbstractModule {
	private MapBinder<String, ModeChoiceAlternative> alternativeBinder;

	@Override
	final public void install() {
		alternativeBinder = MapBinder.newMapBinder(binder(), String.class, ModeChoiceAlternative.class);
		installMode();
	}

	protected LinkedBindingBuilder<ModeChoiceAlternative> addModeAlternativeBinding(String mode) {
		return alternativeBinder.addBinding(mode);
	}

	public abstract void installMode();
}
