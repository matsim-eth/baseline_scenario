package ch.ethz.matsim.baseline_scenario.zurich.router.run;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import ch.ethz.matsim.baseline_scenario.zurich.router.modules.BikeRoutingModule;
import ch.ethz.matsim.baseline_scenario.zurich.router.modules.CarRoutingModule;
import ch.ethz.matsim.baseline_scenario.zurich.router.modules.OutsideRoutingModule;
import ch.ethz.matsim.baseline_scenario.zurich.router.modules.ParallelRouterModule;
import ch.ethz.matsim.baseline_scenario.zurich.router.modules.PublicTransitRoutingModule;
import ch.ethz.matsim.baseline_scenario.zurich.router.modules.WalkRoutingModule;
import ch.ethz.matsim.baseline_scenario.zurich.router.parallel.ParallelPopulationRouter;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorFactory;

public class RunPopulationRouter {
	static public void main(String[] args) throws InterruptedException, ExecutionException {
		String configInputPath = args[0];
		String populationInputPath = args[1];
		String networkInputPath = args[2];
		String facilitiesInputPath = args[3];
		String transitScheduleInputPath = args[4];
		String populationOutputPath = args[5];

		Config config = ConfigUtils.loadConfig(configInputPath);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationInputPath);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkInputPath);
		new MatsimFacilitiesReader(scenario).readFile(facilitiesInputPath);
		new TransitScheduleReader(scenario).readFile(transitScheduleInputPath);

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));

		ModeRoutingParams outsideModeRoutingParams = config.plansCalcRoute().getOrCreateModeRoutingParams("outside");
		outsideModeRoutingParams.setBeelineDistanceFactor(1.0);
		outsideModeRoutingParams.setTeleportedModeSpeed(1e6);

		ParallelPopulationRouter router = Guice
				.createInjector(new ParallelRouterModule(4, scenario.getActivityFacilities()),
						// new SequentialRouterModule(scenario.getActivityFacilities()),
						new CarRoutingModule(roadNetwork),
						new PublicTransitRoutingModule(scenario.getNetwork(), scenario.getTransitSchedule(),
								config.plansCalcRoute().getModeRoutingParams().get("walk")),
						new BikeRoutingModule(config.plansCalcRoute().getModeRoutingParams().get("bike")),
						new WalkRoutingModule(config.plansCalcRoute().getModeRoutingParams().get("walk")),
						new OutsideRoutingModule(outsideModeRoutingParams), new AbstractModule() {
							@Override
							protected void configure() {
								bind(StageActivityTypes.class)
										.toInstance(new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
								bind(MainModeIdentifier.class).toInstance(new MainModeIdentifierImpl());
								bind(Config.class).toInstance(config);
								bind(TransitRouter.class).toProvider(SwissRailRaptorFactory.class);
							}
						})
				.getInstance(ParallelPopulationRouter.class);

		ExecutorService executor = Executors.newFixedThreadPool(4);
		router.run(scenario.getPopulation(), executor);
		executor.shutdown();

		new PopulationWriter(scenario.getPopulation()).write(populationOutputPath);
	}
}
