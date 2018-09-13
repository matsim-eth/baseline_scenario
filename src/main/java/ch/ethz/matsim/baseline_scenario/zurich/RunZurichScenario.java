package ch.ethz.matsim.baseline_scenario.zurich;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
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
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import com.google.inject.Provides;

import ch.ethz.matsim.baseline_scenario.BaselineModule;
import ch.ethz.matsim.baseline_scenario.config.CommandLine;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;
import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class RunZurichScenario {
	static public void main(String[] args) throws ConfigurationException {
		// The general boilerplate code
		CommandLine cmd = new CommandLine.Builder(args).build();

		PSimConfigGroup pSimConfigGroup = new PSimConfigGroup();
		SBBTransitConfigGroup sbbTransitConfig = new SBBTransitConfigGroup();

		Config config = ConfigUtils.loadConfig(cmd.getPositionalArgumentStrict(0), pSimConfigGroup, sbbTransitConfig);
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);

		// Just for compatibility with SwissRailRaptor (we have some special routes).
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
		
		// Setting up SwissRailRaptor + SBB Transit + PSim from here
		
		config.controler().setWriteEventsInterval(1);

		config.parallelEventHandling().setSynchronizeOnSimSteps(false);
		config.parallelEventHandling().setNumberOfThreads(1);

		pSimConfigGroup.setFullTransitPerformanceTransmission(false);
		pSimConfigGroup.setIterationsPerCycle(2);

		sbbTransitConfig.setDeterministicServiceModes(new HashSet<>(Arrays.asList("rail")));
		sbbTransitConfig.setCreateLinkEventsInterval(4);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new BaselineModule()); // Not important (just for the test scenario)
		controler.addOverridingModule(new ZurichModule()); // Not important (just for the test scenario)

		controler.addOverridingModule(new SwissRailRaptorModule());
		controler.addOverridingModule(new SBBTransitModule());

		MobSimSwitcher mobSimSwitcher = new MobSimSwitcher(pSimConfigGroup, scenario);
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

		// Make QSim use SBB Transit
		controler.addOverridingModule(new AbstractModule() {
			@Provides
			QSimComponents provideQSimComponents() {
				QSimComponents components = new QSimComponents();
				new StandardQSimComponentsConfigurator(config).configure(components);
				SBBTransitEngineQSimModule.configure(components);
				return components;
			}

			@Override
			public void install() {
			}
		});

		controler.run();
	}
}
