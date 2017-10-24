package ch.ethz.matsim.baseline_scenario.utils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.DailyReferenceCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.HourlyReferenceCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.utils.HourlyCountsAggregator;
import ch.ethz.matsim.baseline_scenario.utils.routing.CarRouting;

public class ExtendedLocationChoice {

	public static void main(String[] args) throws IOException, InterruptedException {
		String populationInput = "/home/sebastian/baseline_scenario/data/output_population.xml.gz";
		String networkInput = "/home/sebastian/baseline_scenario/data/output_network.xml.gz";
		String astraCountsInput = "/home/sebastian/temp/streetCounts_ASTRA.csv";
		String ktzhCountsInput = "/home/sebastian/temp/streetCounts_KtZH.csv";
		
		double scaling = 0.001;
		
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkInput);
		
		Collection<DailyCountItem> countsItems = new LinkedList<>();
		
		countsItems.addAll(new DailyReferenceCountsReader(network).read(ktzhCountsInput));
		countsItems.addAll(new HourlyCountsAggregator().aggregate(new HourlyReferenceCountsReader(network).read(astraCountsInput)));
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationInput);
		
		Collection<Person> persons = new LinkedList<>(scenario.getPopulation().getPersons().values());
		
		System.out.println("Setting up choice problem ...");
		LocationPlanChoiceProblem choiceProblem = new LocationPlanChoiceProblem(scaling, countsItems, persons);
		TravelTime previousTravelTime = new FreeSpeedTravelTime();
		CarRouting routing = new CarRouting(4, network);
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/sebastian/temp/dists.txt")));
		
		int[] reference = choiceProblem.getReference();
		for (int k = 0; k < reference.length; k++) writer.write(reference[k] + " ");
		writer.write("\n");
		writer.flush();
		
		for (int u = 0; u < 100; u++) {			
			System.out.println("Updating choice problem ...");
			choiceProblem.update();
			
			System.out.println("Solving choice problem ...");
			choiceProblem.solve();
			
			System.out.println("Objective: " + choiceProblem.getObjective());
			
			int[] counts = choiceProblem.getCounts();
			for (int k = 0; k < counts.length; k++) writer.write(counts[k] + " ");
			writer.write("\n");
			writer.flush();
			
			System.out.println("Calculating new travel time ...");
			TravelTime travelTime = new CountTravelTime(scaling, network, persons, previousTravelTime);
			
			System.out.println("Routing ...");
			routing.run(persons, travelTime);
			
			previousTravelTime = travelTime;
		}
		
		writer.close();
	}
}
