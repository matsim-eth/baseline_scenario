package ch.ethz.matsim.baseline_scenario;

import ch.ethz.matsim.baseline_scenario.config.CommandLine;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;
import ch.ethz.matsim.baseline_scenario.transit.BaselineTransitModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSwitzerlandScenario {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args).build();

		Config config = ConfigUtils.loadConfig(cmd.getPositionalArgumentStrict(0));
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new BaselineModule());
		controler.addOverridingModule(new BaselineTransitModule());

		controler.run();
	}
}
