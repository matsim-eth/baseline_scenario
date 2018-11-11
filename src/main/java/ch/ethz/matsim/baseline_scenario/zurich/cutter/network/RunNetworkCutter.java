package ch.ethz.matsim.baseline_scenario.zurich.cutter.network;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import ch.ethz.matsim.baseline_scenario.zurich.extent.CircularScenarioExtent;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class RunNetworkCutter {
	static public void main(String[] args) {
		String networkInputPath = args[0];
		String populationInputPath = args[1];
		String transitScheduleInputPath = args[2];
		String networkOutputPath = args[3];

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkInputPath);

		Coord bellevue = new Coord(2683253.0, 1246745.0);
		ScenarioExtent extent = new CircularScenarioExtent(bellevue, 30000.0);

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));

		Link referenceLink = NetworkUtils.getNearestLink(roadNetwork, bellevue);

		Collection<LeastCostPathCalculator> calculators = new LinkedList<>();

		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);

		for (int i = 0; i < 4; i++) {
			calculators.add(new DijkstraFactory().createPathCalculator(roadNetwork, travelDisutility, travelTime));
		}

		File cacheFile = new File("minimum_network.cache");

		ExecutorService executor = Executors.newFixedThreadPool(4);
		MinimumNetworkFinder minimumNetworkFinder = new ParallelMinimumNetworkFinder(executor, 4, roadNetwork,
				referenceLink);
		MinimumNetworkFinder cachedMinimumNetworkFinder = new CachedMinimumNetworkFinder(cacheFile,
				minimumNetworkFinder);

		NetworkCutter networkCutter = new NetworkCutter(extent, cachedMinimumNetworkFinder);

		new PopulationReader(scenario).readFile(populationInputPath);
		new TransitScheduleReader(scenario).readFile(transitScheduleInputPath);

		networkCutter.run(scenario.getPopulation(), scenario.getTransitSchedule(), scenario.getNetwork());
		executor.shutdown();

		new NetworkWriter(scenario.getNetwork()).write(networkOutputPath);
	}
}
