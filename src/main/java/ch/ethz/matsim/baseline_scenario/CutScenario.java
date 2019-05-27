package ch.ethz.matsim.baseline_scenario;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
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
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import ch.ethz.matsim.baseline_scenario.config.CommandLine;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;
import ch.ethz.matsim.baseline_scenario.preparation.Routing;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import ch.ethz.matsim.baseline_scenario.transit.routing.EnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.utils.HomeFacilitiesCleaner;
import ch.ethz.matsim.baseline_scenario.utils.HouseholdsCleaner;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.config.ConfigCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.connector.ClosestLinkOutsideConnector;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.facilities.FacilitiesCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.network.MinimumNetworkFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.network.NetworkCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.network.ParallelMinimumNetworkFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.PlanCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.DefaultNetworkCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.DefaultTeleportationCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.DefaultTransitRouteCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.DefaultTransitTripCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.NetworkCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TeleportationCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TransitRouteCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TransitTripCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.CarTripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.ModeAwareTripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.PublicTransitTripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.TeleportationTripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.TripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.schedule.DefaultStopSequenceCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.schedule.StopSequenceCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.schedule.TransitScheduleCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.schedule.TransitVehiclesCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DefaultMergeOutsideActivities;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.MergeOutsideActivities;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.RemoveEmptyPlans;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ShapeScenarioExtent;
import ch.ethz.matsim.baseline_scenario.zurich.utils.AdjustLinkLengths;
import ch.ethz.matsim.baseline_scenario.zurich.utils.OutsideAttributeSetter;

public class CutScenario {
	static public void main(String[] args)
			throws ConfigurationException, MalformedURLException, IOException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-config-path", "shapefile-path", "shapefile-attribute", "shapefile-value",
						"output-path", "prefix") //
				.allowOptions("threads", "local-buffer-size", "population-path", "network-path") //
				.build();

		String prefix = cmd.getOptionStrict("prefix");
		String inputConfigPath = cmd.getOptionStrict("input-config-path");
		File shapefilePath = new File(cmd.getOptionStrict("shapefile-path"));
		String shapefileAttribute = cmd.getOptionStrict("shapefile-attribute");
		String shapefileValue = cmd.getOptionStrict("shapefile-value");
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		int localBufferSize = cmd.getOption("local-buffer-size").map(Integer::parseInt).orElse(100);
		File outputPath = new File(cmd.getOptionStrict("output-path"));

		if (!outputPath.isDirectory()) {
			throw new IllegalStateException();
		}

		Config config = ConfigUtils.loadConfig(inputConfigPath);

		if (cmd.hasOption("population-path")) {
			config.plans().setInputFile(cmd.getOptionStrict("population-path"));
		}
		
		if (cmd.hasOption("network-path")) {
			config.network().setInputFile(cmd.getOptionStrict("network-path"));
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(EnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		ScenarioExtent scenarioExtent = new ShapeScenarioExtent.Builder(shapefilePath, shapefileAttribute,
				shapefileValue).build();

		if (!checkRouting(scenario.getPopulation())) {
			throw new IllegalStateException("Input population should be routed completely.");
		}

		// Prepare cutting

		StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
		MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();

		Map<String, TripProcessor> tripProcessors = new HashMap<>();

		TeleportationCrossingPointFinder teleportationCrossingPointFinder = new DefaultTeleportationCrossingPointFinder(
				scenarioExtent);

		tripProcessors.put(TransportMode.walk,
				new TeleportationTripProcessor(teleportationCrossingPointFinder, scenarioExtent));
		tripProcessors.put(TransportMode.bike,
				new TeleportationTripProcessor(teleportationCrossingPointFinder, scenarioExtent));

		// TODO: Measure this from baseline!
		TravelTime networkTravelTime = new FreeSpeedTravelTime();
		NetworkCrossingPointFinder networkCrossingPointFinder = new DefaultNetworkCrossingPointFinder(scenarioExtent,
				scenario.getNetwork(), networkTravelTime);
		tripProcessors.put(TransportMode.car, new CarTripProcessor(networkCrossingPointFinder, scenarioExtent));

		tripProcessors.put("car_passenger",
				new CarTripProcessor(networkCrossingPointFinder, scenarioExtent, "car_passenger"));
		tripProcessors.put("prav3", new CarTripProcessor(networkCrossingPointFinder, scenarioExtent, "prav3"));
		tripProcessors.put("prav4", new CarTripProcessor(networkCrossingPointFinder, scenarioExtent, "prav4"));
		tripProcessors.put("prav5", new CarTripProcessor(networkCrossingPointFinder, scenarioExtent, "prav5"));

		TransitRouteCrossingPointFinder transitRouteCrossingPointFinder = new DefaultTransitRouteCrossingPointFinder(
				scenarioExtent, scenario.getTransitSchedule());
		TransitTripCrossingPointFinder transitTripCrossingPointFinder = new DefaultTransitTripCrossingPointFinder(
				transitRouteCrossingPointFinder, teleportationCrossingPointFinder);
		tripProcessors.put(TransportMode.pt,
				new PublicTransitTripProcessor(transitTripCrossingPointFinder, scenarioExtent, 0.0));

		TripProcessor tripProcessor = new ModeAwareTripProcessor(mainModeIdentifier, tripProcessors);

		PlanCutter planCutter = new PlanCutter(tripProcessor, scenarioExtent, stageActivityTypes);
		MergeOutsideActivities mergeOutsideActivities = new DefaultMergeOutsideActivities();

		// Cut plans

		Iterator<? extends Person> personIterator = scenario.getPopulation().getPersons().values().iterator();
		long totalNumberOfPersons = scenario.getPopulation().getPersons().size();
		AtomicLong processedNumberOfPersons = new AtomicLong(0L);
		List<Thread> threads = new LinkedList<>();

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(() -> {
				List<Person> localTasks = new LinkedList<>();

				do {
					localTasks.clear();

					synchronized (personIterator) {
						while (personIterator.hasNext() && localTasks.size() < localBufferSize) {
							localTasks.add(personIterator.next());
						}
					}

					for (Person person : localTasks) {
						List<Plan> newPlans = new LinkedList<>();
						List<Plan> oldPlans = new LinkedList<>();

						for (Plan oldPlan : person.getPlans()) {
							List<PlanElement> newPlanElements = planCutter.processPlan(oldPlan.getPlanElements());
							mergeOutsideActivities.run(newPlanElements);

							Plan newPlan = scenario.getPopulation().getFactory().createPlan();

							for (int k = 0; k < newPlanElements.size(); k++) {
								if (k % 2 == 0) {
									newPlan.addActivity((Activity) newPlanElements.get(k));
								} else {
									newPlan.addLeg((Leg) newPlanElements.get(k));
								}
							}

							newPlans.add(newPlan);
							oldPlans.add(oldPlan);
						}

						oldPlans.forEach(person::removePlan);
						newPlans.forEach(person::addPlan);
					}

					processedNumberOfPersons.addAndGet(localTasks.size());
				} while (localTasks.size() > 0);
			});

			threads.add(thread);
		}

		for (Thread thread : threads) {
			thread.start();
		}

		Thread progressThread = new Thread(() -> {
			try {
				long currentNumberOfPersons = 0;

				while (currentNumberOfPersons < totalNumberOfPersons) {
					currentNumberOfPersons = processedNumberOfPersons.get();

					System.out.println(String.format("Cutting plans... %d/%d (%.2f%%)", currentNumberOfPersons,
							totalNumberOfPersons, 100.0 * currentNumberOfPersons / totalNumberOfPersons));

					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
			}

			if (totalNumberOfPersons == processedNumberOfPersons.get()) {
				System.out.println("Cutting plans... Done!");
			}
		});

		progressThread.start();

		for (Thread thread : threads) {
			thread.join();
		}

		progressThread.interrupt();
		progressThread.join();

		// Some cleanup after cutting

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));

		new ClosestLinkOutsideConnector(scenario.getPopulation()).run(scenario.getActivityFacilities(),
				scenario.getNetwork(), roadNetwork);

		new RemoveEmptyPlans().run(scenario.getPopulation());

		Network updatedRoadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(updatedRoadNetwork, Collections.singleton("car"));
		Link referenceLink = NetworkUtils.getNearestLink(updatedRoadNetwork, scenarioExtent.getReferencePoint());

		// Cut the public transit supply

		StopSequenceCrossingPointFinder stopSequenceCrossingPointFinder = new DefaultStopSequenceCrossingPointFinder(
				scenarioExtent);
		new TransitScheduleCutter(scenarioExtent, stopSequenceCrossingPointFinder).run(scenario.getTransitSchedule());
		new TransitVehiclesCutter(scenario.getTransitSchedule()).run(scenario.getTransitVehicles());

		// Cut the network

		ExecutorService mainExecutor = Executors.newFixedThreadPool(numberOfThreads);

		MinimumNetworkFinder minimumNetworkFinder = new ParallelMinimumNetworkFinder(mainExecutor, numberOfThreads,
				updatedRoadNetwork, referenceLink);

		new NetworkCutter(scenarioExtent, minimumNetworkFinder).run(scenario.getPopulation(),
				scenario.getTransitSchedule(), scenario.getNetwork());

		new AdjustLinkLengths(10.0).run(scenario.getNetwork());

		// Cut households

		new HouseholdsCleaner(scenario.getPopulation().getPersons().keySet()).run(scenario.getHouseholds());

		// Cut facilities

		new HomeFacilitiesCleaner(scenario.getHouseholds().getHouseholds().keySet(),
				scenario.getPopulation().getPersons().values()).run(scenario.getActivityFacilities());
		new FacilitiesCutter(scenarioExtent, scenario.getPopulation().getPersons().values())
				.run(scenario.getActivityFacilities(), false);

		mainExecutor.shutdown();

		// Adapt config
		new ConfigCutter(prefix).run(config);

		// Routing
		Network finalRoadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(finalRoadNetwork, Collections.singleton("car"));

		Routing routing = new Routing(config, scenario.getNetwork(), finalRoadNetwork, scenario.getTransitSchedule());
		routing.run(scenario.getPopulation());

		// Additional stages
		new OutsideAttributeSetter(scenario.getNetwork()).run(scenario.getPopulation());

		// Write

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

	static private boolean checkRouting(Population population) {
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;

						if (leg.getRoute() == null) {
							return false;
						}
					}
				}
			}
		}

		return true;
	}
}
