package ch.ethz.matsim.baseline_scenario.zurich;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculator;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.contrib.pseudosimulation.mobsim.SwitchingMobsimProvider;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.deterministic.DeterministicStopStopTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.deterministic.DeterministicWaitTimeCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import ch.ethz.matsim.baseline_scenario.BaselineModule;
import ch.ethz.matsim.baseline_scenario.config.CommandLine;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class RunZurichScenario {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args).build();

		PSimConfigGroup pSimConfigGroup = new PSimConfigGroup();
		pSimConfigGroup.setFullTransitPerformanceTransmission(false);
		pSimConfigGroup.setIterationsPerCycle(10);
		
		Config config = ConfigUtils.loadConfig(cmd.getPositionalArgumentStrict(0), pSimConfigGroup);
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(scenario);
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;
						
						if (leg.getMode().equals("pt")) {
							leg.setRoute(null);
						}
					}
				}
			}
		}
		
		config.parallelEventHandling().setSynchronizeOnSimSteps(false);
		config.parallelEventHandling().setNumberOfThreads(1);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SwissRailRaptorModule());
		controler.addOverridingModule(new BaselineModule());
		//controler.addOverridingModule(new BaselineTransitModule());
		controler.addOverridingModule(new ZurichModule());
		
		MobSimSwitcher mobSimSwitcher = new MobSimSwitcher(pSimConfigGroup,scenario);
		controler.addControlerListener(mobSimSwitcher);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// Taken from RunPSim
				bind(MobSimSwitcher.class).toInstance(mobSimSwitcher);
				bindMobsim().toProvider(SwitchingMobsimProvider.class);
				bind(TravelTimeCalculator.class).to(PSimTravelTimeCalculator.class);
				bind(TravelTime.class).toProvider(PSimTravelTimeCalculator.class);
				bind(PlanCatcher.class).toInstance(new PlanCatcher());
				bind(PSimProvider.class).toInstance(new PSimProvider(scenario, controler.getEvents()));
				
				// Replace from standard PSim: Use deterministic calculation
				bind(WaitTimeCalculator.class).to(DeterministicWaitTimeCalculator.class);
				bind(WaitTime.class).toProvider(DeterministicWaitTimeCalculator.class);
				bind(StopStopTimeCalculator.class).to(DeterministicStopStopTimeCalculator.class);
				bind(StopStopTime.class).toProvider(DeterministicStopStopTimeCalculator.class);

				// Make PSim use the "MATSim-configured" QSim, instead of a purely custom one!
				bind(QSimProvider.class);
			}
		});

		controler.run();
	}
}
