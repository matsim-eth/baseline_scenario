package ch.ethz.matsim.baseline_scenario.utils.counts;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.utils.compatibility.DeprecatedDailyReferenceCountsReader;
import ch.ethz.matsim.baseline_scenario.utils.routing.CarRouting;

public class TrafficCountPlanSelector {
	final private double scaling;
	final private double rerouting;
	final private int numberOfThreads;
	final private int maximumNumberOfIterations;

	final private Network network;
	final private Collection<DailyCountItem> countItems;

	final private Logger logger = Logger.getLogger(TrafficCountPlanSelector.class);
	final private BufferedWriter countWriter;

	public TrafficCountPlanSelector(Network network, Collection<DailyCountItem> countItems, double scaling,
			double rerouting, int numberOfThreads, String countOutputPath, int maximumNumberOfIterations)
			throws FileNotFoundException {
		this.network = network;
		this.countItems = countItems;
		this.scaling = scaling;
		this.rerouting = rerouting;
		this.numberOfThreads = numberOfThreads;
		this.countWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(countOutputPath)));
		this.maximumNumberOfIterations = maximumNumberOfIterations;
	}

	public void run(Population population) throws IOException, InterruptedException {
		List<Person> persons = new LinkedList<>(population.getPersons().values());

		logger.info("Setting up choice problem ...");
		CountFittingProblem choiceProblem = new CountFittingProblem(scaling, countItems, persons);
		TravelTime previousTravelTime = new FreeSpeedTravelTime();
		CarRouting routing = new CarRouting(numberOfThreads, network);

		int[] reference = choiceProblem.getReference();

		for (int k = 0; k < reference.length; k++)
			countWriter.write(reference[k] + " ");
		countWriter.write("\n");
		countWriter.flush();

		Random random = new Random();
		Collection<Person> activePersons = persons;

		double previousObjective = Double.POSITIVE_INFINITY;
		double currentObjective = Double.NaN;

		int u = 0;

		while (previousObjective != currentObjective && u < maximumNumberOfIterations) {
			logger.info("Iteration " + u++);
			logger.info("  Updating choice problem ...");
			choiceProblem.update(activePersons);

			int[] counts = choiceProblem.getCounts();
			for (int k = 0; k < counts.length; k++)
				countWriter.write(counts[k] + " ");
			countWriter.write("\n");
			countWriter.flush();

			logger.info("  Solving choice problem ...");
			choiceProblem.solve();

			logger.info("    Objective: " + choiceProblem.getObjective());
			previousObjective = currentObjective;
			currentObjective = choiceProblem.getObjective();

			logger.info("  Calculating new travel time ...");
			TravelTime travelTime = new CountTravelTime(scaling, network, persons, previousTravelTime);

			int n = (int) (persons.size() * rerouting);
			int start = random.nextInt(persons.size() - n);

			logger.info("  Rerouting ...");
			activePersons = persons.subList(start, start + n);
			routing.run(activePersons, travelTime);

			previousTravelTime = travelTime;
		}

		countWriter.close();
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		String populationInput = args[0];
		String networkInput = args[1];
		String dailyCountsInput = args[2];
		String countOutputPath = args[3];

		double scaling = Double.parseDouble(args[4]);
		double rerouting = Double.parseDouble(args[5]);
		int numberOfThreads = Integer.parseInt(args[6]);
		int maximumNumberOfIterations = Integer.parseInt(args[7]);

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkInput);

		Collection<DailyCountItem> countItems = new DeprecatedDailyReferenceCountsReader(network).read(dailyCountsInput);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationInput);

		new TrafficCountPlanSelector(network, countItems, scaling, rerouting, numberOfThreads, countOutputPath,
				maximumNumberOfIterations).run(scenario.getPopulation());
	}
}
