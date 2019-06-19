package ch.ethz.matsim.baseline_scenario.zurich;

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
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
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

import ch.ethz.matsim.baseline_scenario.config.SwitzerlandConfig;
import ch.ethz.matsim.baseline_scenario.config.ZurichScenarioConfig;
import ch.ethz.matsim.baseline_scenario.preparation.Routing;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import ch.ethz.matsim.baseline_scenario.utils.HomeFacilitiesCleaner;
import ch.ethz.matsim.baseline_scenario.utils.HouseholdsCleaner;
import ch.ethz.matsim.baseline_scenario.utils.consistency.MD5Collector;
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

public class MakeParisScenario {
	public static void main(String[] args) throws Exception {
		ObjectMapper json = new ObjectMapper();
		json.enable(SerializationFeature.INDENT_OUTPUT);

		ZurichScenarioConfig scenarioConfig = json.readValue(new File(args[0]), ZurichScenarioConfig.class);

		File baselinePath = new File(scenarioConfig.baselinePath);
		File outputPath = new File(scenarioConfig.outputPath);

		if (!baselinePath.exists()) {
			throw new IllegalArgumentException("Input path does not exist: " + baselinePath);
		}

		outputPath.mkdirs();

		// SwitzerlandConfig baselineConfig = json.readValue(
		// new File(baselinePath, scenarioConfig.baselinePrefix + "make_config.json"),
		// SwitzerlandConfig.class);

		SwitzerlandConfig baselineConfig = new SwitzerlandConfig();
		baselineConfig.prefix = scenarioConfig.baselinePrefix;

		MD5Collector baselineFilesCollector = new MD5Collector(baselinePath);
		MD5Collector outputFilesCollector = new MD5Collector(outputPath);

		int numberOfThreads = scenarioConfig.numberOfThreads == 0 ? Runtime.getRuntime().availableProcessors()
				: scenarioConfig.numberOfThreads;

		Config config = ConfigUtils.loadConfig(new File(baselinePath, baselineConfig.prefix + "config.xml").getPath());
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(scenario);
		/*
		 * new TransitScheduleReader(scenario).readFile("1pm/" +
		 * config.transit().getTransitScheduleFile());
		 * 
		 * { Coord cityCenter = new Coord(652127.0, 6861007.0); ScenarioExtent extent =
		 * new CircularScenarioExtent(scenario.getNetwork(), cityCenter, 10000.0);
		 * 
		 * StopSequenceCrossingPointFinder stopSequenceCrossingPointFinder = new
		 * DefaultStopSequenceCrossingPointFinder( extent); new
		 * TransitScheduleCutter(extent,
		 * stopSequenceCrossingPointFinder).run(scenario.getTransitSchedule());
		 * 
		 * System.exit(1); }
		 */

		baselineFilesCollector.add(baselineConfig.prefix + "population.xml.gz");
		baselineFilesCollector.add(baselineConfig.prefix + "households.xml.gz");
		baselineFilesCollector.add(baselineConfig.prefix + "facilities.xml.gz");
		baselineFilesCollector.add(baselineConfig.prefix + "network.xml.gz");
		baselineFilesCollector.add(baselineConfig.prefix + "transit_schedule.xml.gz");
		baselineFilesCollector.add(baselineConfig.prefix + "transit_vehicles.xml.gz");
		baselineFilesCollector.add(baselineConfig.prefix + "config.xml");
		// baselineFilesCollector.add(baselineConfig.prefix + "make_config.json");

		// Paris: 652127.0, 6861007.0, radius 10000.0
		// Zurich: 2683253.0, 1246745.0, radius 30000.0

		Coord cityCenter = new Coord(scenarioConfig.centerX, scenarioConfig.centerY);
		ScenarioExtent extent = new CircularScenarioExtent(cityCenter, scenarioConfig.scenarioRadius);

		StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
		MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();
		ExecutorService mainExecutor = Executors.newFixedThreadPool(numberOfThreads);

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));
		Link referenceLink = NetworkUtils.getNearestLink(roadNetwork, extent.getReferencePoint());

		// Perform a first rerouting of the whole population

		Config routingConfig = ConfigUtils
				.loadConfig(new File(baselinePath, baselineConfig.prefix + "config.xml").getPath());

		ModeRoutingParams outsideModeRoutingParams = routingConfig.plansCalcRoute()
				.getOrCreateModeRoutingParams("outside");
		outsideModeRoutingParams.setBeelineDistanceFactor(1.0);
		outsideModeRoutingParams.setTeleportedModeSpeed(1e6);

		/*
		 * Config routingConfig = ConfigUtils .loadConfig(new File(baselinePath,
		 * baselineConfig.prefix + "config.xml").getPath());
		 * 
		 * ModeRoutingParams outsideModeRoutingParams = routingConfig.plansCalcRoute()
		 * .getOrCreateModeRoutingParams("outside");
		 * outsideModeRoutingParams.setBeelineDistanceFactor(1.0);
		 * outsideModeRoutingParams.setTeleportedModeSpeed(1e6);
		 * 
		 * ParallelPopulationRouter populationRouter = Guice.createInjector( new
		 * ParallelRouterModule(numberOfThreads, scenario.getActivityFacilities()), new
		 * AbstractModule() {
		 * 
		 * @Override protected void configure() {
		 * bind(StageActivityTypes.class).toInstance(stageActivityTypes);
		 * bind(MainModeIdentifier.class).toInstance(mainModeIdentifier);
		 * bind(TransitRouter.class).toProvider(SwissRailRaptorFactory.class);
		 * bind(Config.class).toInstance(routingConfig); } }, new
		 * CarRoutingModule(roadNetwork), new
		 * PublicTransitRoutingModule(scenario.getNetwork(),
		 * scenario.getTransitSchedule(),
		 * config.plansCalcRoute().getOrCreateModeRoutingParams("walk")), new
		 * BikeRoutingModule(config.plansCalcRoute().getOrCreateModeRoutingParams("bike"
		 * )), new
		 * WalkRoutingModule(config.plansCalcRoute().getOrCreateModeRoutingParams("walk"
		 * )), new OutsideRoutingModule(outsideModeRoutingParams)).getInstance(
		 * ParallelPopulationRouter.class);
		 * 
		 * populationRouter.run(scenario.getPopulation(), mainExecutor);
		 */

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

		new Routing(routingConfig, scenario.getNetwork(), updatedRoadNetwork, scenario.getTransitSchedule())
				.run(scenario.getPopulation());

		/*
		 * populationRouter = Guice.createInjector( new
		 * ParallelRouterModule(numberOfThreads, scenario.getActivityFacilities()), new
		 * AbstractModule() {
		 * 
		 * @Override protected void configure() {
		 * bind(StageActivityTypes.class).toInstance(stageActivityTypes);
		 * bind(MainModeIdentifier.class).toInstance(mainModeIdentifier);
		 * bind(TransitRouter.class).toProvider(SwissRailRaptorFactory.class);
		 * bind(Config.class).toInstance(routingConfig); } }, new
		 * CarRoutingModule(updatedRoadNetwork), new
		 * PublicTransitRoutingModule(scenario.getNetwork(),
		 * scenario.getTransitSchedule(),
		 * config.plansCalcRoute().getOrCreateModeRoutingParams("walk")), new
		 * BikeRoutingModule(config.plansCalcRoute().getOrCreateModeRoutingParams("bike"
		 * )), new
		 * WalkRoutingModule(config.plansCalcRoute().getOrCreateModeRoutingParams("walk"
		 * )), new OutsideRoutingModule(outsideModeRoutingParams)).getInstance(
		 * ParallelPopulationRouter.class);
		 * 
		 * populationRouter.run(scenario.getPopulation(), mainExecutor);
		 */

		// Cut the network

		MinimumNetworkFinder minimumNetworkFinder = new ParallelMinimumNetworkFinder(mainExecutor, numberOfThreads,
				updatedRoadNetwork, referenceLink);

		if (scenarioConfig.useMinimumNetworkCache) {
			minimumNetworkFinder = new CachedMinimumNetworkFinder(new File(outputPath, "minimum_network.cache"),
					minimumNetworkFinder);
		}

		new NetworkCutter(extent, minimumNetworkFinder).run(scenario.getPopulation(), scenario.getActivityFacilities(), scenario.getTransitSchedule(),
				scenario.getNetwork());

		new AdjustLinkLengths(10.0).run(scenario.getNetwork());

		// Cut households

		new HouseholdsCleaner(scenario.getPopulation().getPersons().keySet()).run(scenario.getHouseholds());

		// Cut attributes

		/*
		 * Collection<String> householdAttributeNames = new AttributeNamesReader()
		 * .read(new File(baselinePath, baselineConfig.prefix +
		 * "household_attributes.xml.gz")); HouseholdAttributeCleaner
		 * householdAttributesCleaner = new
		 * HouseholdAttributeCleaner(householdAttributeNames); ObjectAttributes
		 * cleanedHouseholdAttributes = householdAttributesCleaner.run(
		 * scenario.getHouseholds().getHouseholds().values(),
		 * scenario.getHouseholds().getHouseholdAttributes());
		 * 
		 * Collection<String> personAttributeNames = new AttributeNamesReader()
		 * .read(new File(baselinePath, baselineConfig.prefix +
		 * "population_attributes.xml.gz")); AttributeCleaner<Person>
		 * personAttributesCleaner = new AttributeCleaner<>(personAttributeNames);
		 * ObjectAttributes cleanedPersonAttributes = personAttributesCleaner
		 * .run(scenario.getPopulation().getPersons().values(),
		 * scenario.getPopulation().getPersonAttributes());
		 */

		// Cut facilities

		new HomeFacilitiesCleaner(scenario.getHouseholds().getHouseholds().keySet(),
				scenario.getPopulation().getPersons().values()).run(scenario.getActivityFacilities());
		new FacilitiesCutter(extent, scenario.getPopulation().getPersons().values())
				.run(scenario.getActivityFacilities(), false);

		mainExecutor.shutdown();

		// Additional stages
		new OutsideAttributeSetter(scenario.getNetwork()).run(scenario.getPopulation());

		// Adapt config
		new ConfigCutter(scenarioConfig.prefix).run(config);

		// Write scenario
		new ConfigWriter(config).write(new File(outputPath, scenarioConfig.prefix + "config.xml").getPath());
		new PopulationWriter(scenario.getPopulation())
				.write(new File(outputPath, scenarioConfig.prefix + "population.xml.gz").getPath());
		/*
		 * new ObjectAttributesXmlWriter(cleanedPersonAttributes) .writeFile(new
		 * File(outputPath, scenarioConfig.prefix +
		 * "population_attributes.xml.gz").getPath());
		 */
		new FacilitiesWriter(scenario.getActivityFacilities())
				.write(new File(outputPath, scenarioConfig.prefix + "facilities.xml.gz").getPath());
		new NetworkWriter(scenario.getNetwork())
				.write(new File(outputPath, scenarioConfig.prefix + "network.xml.gz").getPath());
		new HouseholdsWriterV10(scenario.getHouseholds())
				.writeFile(new File(outputPath, scenarioConfig.prefix + "households.xml.gz").getPath());
		/*
		 * new ObjectAttributesXmlWriter(cleanedHouseholdAttributes) .writeFile(new
		 * File(outputPath, scenarioConfig.prefix +
		 * "household_attributes.xml.gz").getPath());
		 */
		new TransitScheduleWriter(scenario.getTransitSchedule())
				.writeFile(new File(outputPath, scenarioConfig.prefix + "transit_schedule.xml.gz").getPath());
		new VehicleWriterV1(scenario.getTransitVehicles())
				.writeFile(new File(outputPath, scenarioConfig.prefix + "transit_vehicles.xml.gz").getPath());
		json.writeValue(new File(outputPath, scenarioConfig.prefix + "make_config.json"), scenarioConfig);
		baselineFilesCollector.write(new File(outputPath, scenarioConfig.prefix + "baseline.md5"));

		outputFilesCollector.add(scenarioConfig.prefix + "config.xml");
		outputFilesCollector.add(scenarioConfig.prefix + "population.xml.gz");
		// outputFilesCollector.add(scenarioConfig.prefix +
		// "population_attributes.xml.gz");
		outputFilesCollector.add(scenarioConfig.prefix + "facilities.xml.gz");
		outputFilesCollector.add(scenarioConfig.prefix + "network.xml.gz");
		outputFilesCollector.add(scenarioConfig.prefix + "households.xml.gz");
		// outputFilesCollector.add(scenarioConfig.prefix +
		// "household_attributes.xml.gz");
		outputFilesCollector.add(scenarioConfig.prefix + "transit_schedule.xml.gz");
		outputFilesCollector.add(scenarioConfig.prefix + "transit_vehicles.xml.gz");
		outputFilesCollector.add(scenarioConfig.prefix + "make_config.json");
		outputFilesCollector.add(scenarioConfig.prefix + "baseline.md5");

		outputFilesCollector.write(new File(outputPath, scenarioConfig.prefix + "output.md5"));
	}
}
