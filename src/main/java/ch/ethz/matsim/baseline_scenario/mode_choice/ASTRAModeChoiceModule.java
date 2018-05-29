package ch.ethz.matsim.baseline_scenario.mode_choice;

import ch.ethz.matsim.baseline_scenario.config.BaselineConfigGroup;
import ch.ethz.matsim.baseline_scenario.mode_choice.modes.bike.BikeModeModule;
import ch.ethz.matsim.baseline_scenario.mode_choice.modes.car.CarModeModule;
import ch.ethz.matsim.baseline_scenario.mode_choice.modes.outside.OutsideModeModule;
import ch.ethz.matsim.baseline_scenario.mode_choice.modes.public_transit.PublicTransitModeModule;
import ch.ethz.matsim.baseline_scenario.mode_choice.modes.walk.WalkModeModule;
import ch.ethz.matsim.baseline_scenario.scoring.ASTRAScoringParameters;
import ch.ethz.matsim.mode_choice.ModeChoiceModel;
import ch.ethz.matsim.mode_choice.mnl.ModeChoiceAlternative;
import ch.ethz.matsim.mode_choice.replanning.ModeChoiceStrategy;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.PtConstants;

import java.util.*;

public class ASTRAModeChoiceModule extends AbstractModule {
	@Override
	public void install() {
		BaselineConfigGroup astraConfig = (BaselineConfigGroup) getConfig().getModules().get(BaselineConfigGroup.GROUP_NAME);

		install(new WalkModeModule());
		install(new BikeModeModule());
		install(new CarModeModule());
		install(new PublicTransitModeModule());
		install(new OutsideModeModule());

//		if (!astraConfig.isBaseline()) {
//			if (astraConfig.getEnableSharedAVs()) {
//				install(new SAVModeModule());
//			}
//
//			if (astraConfig.getEnablePrivateAVs()) {
//				install(new PrivateAVModeModule());
//			}
//		}

		// to add to config for baseline
		addPlanStrategyBinding("ASTRAModeChoice").toProvider(ModeChoiceStrategy.class);
	}

	@Singleton
	@Provides
	public ASTRAChainAlternatives provideASTRAChainAlternatives(BaselineConfigGroup baselineConfigGroup) {
//		Collection<String> borderModes = baselineConfigGroup.getNoPravAtBorder() ? Collections.emptyList()
//				: Arrays.asList("car", "prav");

		return new ASTRAChainAlternatives(new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE),
				new MainModeIdentifierImpl(), Collections.emptyList());
	}

	@Singleton
	@Provides
	public ModeChoiceModel provideModeChoiceStrategy(ASTRAChainAlternatives chainAlternatives, Network network,
                                                     Map<String, ModeChoiceAlternative> modeAlternatives, ASTRAScoringParameters parameters,
                                                     @Named("road") Network roadNetwork) {

		ASTRAModeChoiceModel model = new ASTRAModeChoiceModel(chainAlternatives, network, new Random(0), parameters,
				roadNetwork, new MainModeIdentifierImpl());

		for (Map.Entry<String, ModeChoiceAlternative> entry : modeAlternatives.entrySet()) {
			model.addModeAlternative(entry.getKey(), entry.getValue());
		}

		return model;
	}
}
