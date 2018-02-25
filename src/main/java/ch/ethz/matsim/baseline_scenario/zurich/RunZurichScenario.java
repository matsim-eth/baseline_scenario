package ch.ethz.matsim.baseline_scenario.zurich;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.baseline_scenario.BaselineModule;
import ch.ethz.matsim.baseline_scenario.transit.BaselineTransitModule;

public class RunZurichScenario {
	static public void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);

		config.global().setNumberOfThreads(Integer.parseInt(args[1]));
		config.qsim().setNumberOfThreads(Integer.parseInt(args[2]));

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		// See MATSIM-766 (https://matsim.atlassian.net/browse/MATSIM-766)
		StrategySettings modeChoiceStrategy = new StrategySettings();
		modeChoiceStrategy.setStrategyName("SubtourModeChoice");
		modeChoiceStrategy.setDisableAfter(-1);
		modeChoiceStrategy.setWeight(0.1);
		config.strategy().addStrategySettings(modeChoiceStrategy);

		config.subtourModeChoice().setChainBasedModes(new String[] { "car", "bike" });
		config.subtourModeChoice().setModes(new String[] { "car", "pt", "bike", "walk" });

		controler.addOverridingModule(new BaselineModule());
		//controler.addOverridingModule(new BaselineTransitModule());
		controler.addOverridingModule(new ZurichModule());

		controler.run();
	}
}
