package ch.ethz.matsim.baseline_scenario.preparation;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

import ch.ethz.matsim.baseline_scenario.config.CommandLine;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import ch.ethz.matsim.baseline_scenario.utils.HomeFacilitiesCleaner;
import ch.ethz.matsim.baseline_scenario.utils.HouseholdsCleaner;
import ch.ethz.matsim.baseline_scenario.zurich.consistency.BatchCheck;
import ch.ethz.matsim.baseline_scenario.zurich.consistency.ChainStructureCheck;
import ch.ethz.matsim.baseline_scenario.zurich.consistency.PlanConsistencyCheck;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.config.ConfigCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.connector.ClosestLinkOutsideConnector;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.facilities.FacilitiesCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.network.CachedMinimumNetworkFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.network.MinimumNetworkFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.network.NetworkCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.network.ParallelMinimumNetworkFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.PlanCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.PlanCutterModule;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.population.DefaultParallelPopulationCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.schedule.DefaultStopSequenceCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.schedule.StopSequenceCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.schedule.TransitScheduleCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.schedule.TransitVehiclesCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DefaultMergeOutsideActivities;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.MergeOutsideActivities;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.RemoveEmptyPlans;
import ch.ethz.matsim.baseline_scenario.zurich.extent.CircularScenarioExtent;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;
import ch.ethz.matsim.baseline_scenario.zurich.utils.AdjustLinkLengths;
import ch.ethz.matsim.baseline_scenario.zurich.utils.OutsideAttributeSetter;

public class Cutter {
	public static void main(String[] args) throws Exception {
		CommandLine cmd = new CommandLine.Builder(args)
				.requireOptions("input-path", "output-path", "output-prefix", "center-x", "center-y", "radius")
				.allowOptions("threads", "use-minimum-network-cache").build();

		ObjectMapper json = new ObjectMapper();
		json.enable(SerializationFeature.INDENT_OUTPUT);

		File outputPath = new File(cmd.getOptionStrict("output-path"));
		outputPath.mkdirs();

		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("input-path"));
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		// Paris: 652127.0, 6861007.0, radius 10000.0
		// Zurich: 2683253.0, 1246745.0, radius 30000.0

		double centerX = Double.parseDouble(cmd.getOptionStrict("center-x"));
		double centerY = Double.parseDouble(cmd.getOptionStrict("center-y"));
		double radius = Double.parseDouble(cmd.getOptionStrict("radius"));

		Coord extentCenter = new Coord(centerX, centerY);
		ScenarioExtent extent = new CircularScenarioExtent(scenario.getNetwork(), extentCenter, radius);

		StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
		MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();
		ExecutorService mainExecutor = Executors.newFixedThreadPool(numberOfThreads);

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));
		Link referenceLink = NetworkUtils.getNearestLink(roadNetwork, extent.getReferencePoint());

		// Adapt config
		String prefix = cmd.getOptionStrict("output-prefix");
		new ConfigCutter(prefix).run(config);

		// Cut the population at the border

		PlanCutter planCutter = Guice
				.createInjector(new PlanCutterModule(scenario.getTransitSchedule()), new AbstractModule() {
					@Override
					protected void configure() {
						bind(StageActivityTypes.class).toInstance(stageActivityTypes);
						bind(MainModeIdentifier.class).toInstance(mainModeIdentifier);
						bind(ScenarioExtent.class).toInstance(extent);
						bind(Network.class).annotatedWith(Names.named("road")).toInstance(roadNetwork);
					}
				}).getInstance(PlanCutter.class);

		MergeOutsideActivities mergeOutsideActivities = new DefaultMergeOutsideActivities();
		DefaultParallelPopulationCutter populationCutter = new DefaultParallelPopulationCutter(planCutter,
				mergeOutsideActivities);
		populationCutter.run(scenario.getPopulation(), mainExecutor);

		new ClosestLinkOutsideConnector(scenario.getPopulation()).run(scenario.getActivityFacilities(),
				scenario.getNetwork(), roadNetwork);

		new RemoveEmptyPlans().run(scenario.getPopulation());

		PlanConsistencyCheck planConsistencyCheck = new BatchCheck(
				new ChainStructureCheck(extent, scenario.getNetwork())// ,
		/* new ActivityCheck(scenario.getNetwork(), scenario.getActivityFacilities()) */);
		scenario.getPopulation().getPersons().values()
				.forEach(p -> planConsistencyCheck.run(p.getSelectedPlan().getPlanElements()));

		// Rebuild road network, because outside connectors have been added

		Network updatedRoadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(updatedRoadNetwork, Collections.singleton("car"));
		referenceLink = NetworkUtils.getNearestLink(updatedRoadNetwork, extent.getReferencePoint());

		// Cut the public transit supply

		StopSequenceCrossingPointFinder stopSequenceCrossingPointFinder = new DefaultStopSequenceCrossingPointFinder(
				extent);
		new TransitScheduleCutter(extent, stopSequenceCrossingPointFinder).run(scenario.getTransitSchedule());
		new TransitVehiclesCutter(scenario.getTransitSchedule()).run(scenario.getTransitVehicles());

		// Reroute the cut population

		new Routing(scenario.getConfig(), scenario.getNetwork(), updatedRoadNetwork, scenario.getTransitSchedule())
				.run(scenario.getPopulation());

		// Cut the network

		MinimumNetworkFinder minimumNetworkFinder = new ParallelMinimumNetworkFinder(mainExecutor, numberOfThreads,
				updatedRoadNetwork, referenceLink);

		if (cmd.getOption("use-minimum-network-cache").map(Boolean::parseBoolean).orElse(true)) {
			minimumNetworkFinder = new CachedMinimumNetworkFinder(new File(outputPath, "minimum_network.cache"),
					minimumNetworkFinder);
		}

		new NetworkCutter(extent, minimumNetworkFinder).run(scenario.getPopulation(), scenario.getTransitSchedule(),
				scenario.getNetwork());

		new AdjustLinkLengths(10.0).run(scenario.getNetwork());

		// Cut households

		new HouseholdsCleaner(scenario.getPopulation().getPersons().keySet()).run(scenario.getHouseholds());

		// Cut facilities

		new HomeFacilitiesCleaner(scenario.getHouseholds().getHouseholds().keySet(),
				scenario.getPopulation().getPersons().values()).run(scenario.getActivityFacilities());
		new FacilitiesCutter(extent, scenario.getPopulation().getPersons().values())
				.run(scenario.getActivityFacilities(), false);

		mainExecutor.shutdown();

		// Additional stages
		new OutsideAttributeSetter(scenario.getNetwork()).run(scenario.getPopulation());

		// Write scenario
		new ConfigWriter(config).write(new File(outputPath, prefix + "config.xml").getPath());
		new PopulationWriter(scenario.getPopulation())
				.write(new File(outputPath, prefix + "population.xml.gz").getPath());
		new FacilitiesWriter(scenario.getActivityFacilities())
				.write(new File(outputPath, prefix + "facilities.xml.gz").getPath());
		new NetworkWriter(scenario.getNetwork()).write(new File(outputPath, prefix + "network.xml.gz").getPath());
		new HouseholdsWriterV10(scenario.getHouseholds())
				.writeFile(new File(outputPath, prefix + "households.xml.gz").getPath());
		new TransitScheduleWriter(scenario.getTransitSchedule())
				.writeFile(new File(outputPath, prefix + "transit_schedule.xml.gz").getPath());
		new VehicleWriterV1(scenario.getTransitVehicles())
				.writeFile(new File(outputPath, prefix + "transit_vehicles.xml.gz").getPath());
	}
}
