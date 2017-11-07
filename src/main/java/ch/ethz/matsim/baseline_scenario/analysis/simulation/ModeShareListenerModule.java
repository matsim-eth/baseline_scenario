package ch.ethz.matsim.baseline_scenario.analysis.simulation;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ModeShareListenerModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(ModeShareListener.class);
	}
	
	@Provides @Singleton
	public ModeShareListener provideModeShareListener(Population population, OutputDirectoryHierarchy hierarchy, MainModeIdentifier mainModeIdentifier) {
		return new ModeShareListener(population, hierarchy, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE), mainModeIdentifier);
	}
}
