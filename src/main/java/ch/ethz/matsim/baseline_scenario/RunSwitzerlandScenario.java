package ch.ethz.matsim.baseline_scenario;

import ch.ethz.matsim.baseline_scenario.config.BaselineConfigGroup;
import ch.ethz.matsim.baseline_scenario.mode_choice.ASTRAModeChoiceModule;
import ch.ethz.matsim.baseline_scenario.scoring.ASTRAScoringModule;
import ch.ethz.matsim.baseline_scenario.utils.AdaptConfigForModeChoice;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.baseline_scenario.transit.BaselineTransitModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;

public class RunSwitzerlandScenario {
	static public void main(String[] args) {

//		CommandLineConfigurator cmd = new CommandLineConfigurator(args,
//				Arrays.asList("flow-efficiency", "freeflow", "no-modechoice", "only-replace-car",
//						"no-prav-at-border", "pt-only-keep", "wt-calculator"));

//		Config config = ConfigUtils.loadConfig(args[0], new BaselineConfigGroup());
		Config config = ConfigUtils.loadConfig(args[0]);

		new AdaptConfigForModeChoice().run(0.1, config);

		config.global().setNumberOfThreads(Integer.parseInt(args[1]));
		config.qsim().setNumberOfThreads(Integer.parseInt(args[2]));

		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new BaselineModule());
		controler.addOverridingModule(new BaselineTransitModule());

		controler.addOverridingModule(new ASTRAScoringModule());
		controler.addOverridingModule(new ASTRAModeChoiceModule());

		controler.run();
	}
}
