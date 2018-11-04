package ch.ethz.matsim.baseline_scenario.location_assignment;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.pt.PtConstants;

import ch.ethz.matsim.location_assignment.matsim.MATSimAssignmentProblem;
import ch.ethz.matsim.location_assignment.matsim.discretizer.FacilityTypeDiscretizerFactory;
import ch.ethz.matsim.location_assignment.matsim.solver.MATSimAssignmentSolver;
import ch.ethz.matsim.location_assignment.matsim.solver.MATSimAssignmentSolverBuilder;
import ch.ethz.matsim.location_assignment.matsim.utils.LocationAssignmentPlanAdapter;

public class BaselineLocationAssignment {
	static public void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		String facilitiesInputPath = args[0];
		String populationInputPath = args[1];
		String quantilesPath = args[2];
		String distributionsPath = args[3];
		String outputPath = args[4];
		int numberOfThreads = Integer.parseInt(args[5]);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new MatsimFacilitiesReader(scenario).readFile(facilitiesInputPath);
		new PopulationReader(scenario).readFile(populationInputPath);

		new BaselineLocationAssignment().run(scenario.getPopulation(), scenario.getActivityFacilities(), quantilesPath,
				distributionsPath, numberOfThreads);

		new PopulationWriter(scenario.getPopulation()).write(outputPath);
	}

	public void run(Population population, ActivityFacilities facilities, String quantilesPath,
			String distributionsPath, int numberOfThreads)
			throws IOException, InterruptedException, ExecutionException {
		// CONFIGURATION

		int discretizationIterations = 1000;

		Set<String> relevantActivityTypes = new HashSet<>(Arrays.asList("leisure", "shop", "service"));

		Map<String, Double> discretizationThresholds = new HashMap<>();
		discretizationThresholds.put("car", 200.0);
		discretizationThresholds.put("pt", 200.0);
		discretizationThresholds.put("bike", 100.0);
		discretizationThresholds.put("walk", 100.0);

		// LOAD POPULATION & FACILITIES

		StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
		MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();

		new ZurichPopulationCleaner().run(population, stageActivityTypes, mainModeIdentifier);

		Random random = new Random(0);

		// SET UP DISTANCE SAMPLERS

		FacilityTypeDiscretizerFactory discretizerFactory = new FacilityTypeDiscretizerFactory(relevantActivityTypes);
		discretizerFactory.loadFacilities(facilities);

		ZurichDistanceSamplerFactory distanceSamplerFactory = new ZurichDistanceSamplerFactory(random);
		distanceSamplerFactory.load(quantilesPath, distributionsPath);

		// set up zurich specifics

		Optional<OutputStream> statisticsStream = Optional.empty();

		// ZurichStatistics zurichStatistics = new
		// ZurichStatistics(scenario.getPopulation().getPersons().size(),
		// statisticsStream);
		ZurichProblemProvider zurichProblemProvider = new ZurichProblemProvider(distanceSamplerFactory,
				discretizerFactory, discretizationThresholds);

		// set up the algorithm

		MATSimAssignmentSolverBuilder builder = new MATSimAssignmentSolverBuilder();

		builder.setVariableActivityTypes(relevantActivityTypes);
		builder.setRandom(random);
		builder.setStageActivityTypes(stageActivityTypes);

		builder.setDiscretizerProvider(zurichProblemProvider);
		builder.setDistanceSamplerProvider(zurichProblemProvider);
		builder.setDiscretizationThresholdProvider(zurichProblemProvider);

		builder.setMaximumDiscretizationIterations(discretizationIterations);

		MATSimAssignmentSolver solver = builder.build();

		long totalNumberOfPersons = population.getPersons().size();
		AtomicLong processedNumberOfPersons = new AtomicLong(0);

		// loop population

		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();
		List<Thread> threads = new LinkedList<>();
		int chunkSize = 10000;

		for (int i = 0; i < numberOfThreads; i++) {
			threads.add(new Thread(() -> {
				LocationAssignmentPlanAdapter adapter = new LocationAssignmentPlanAdapter();

				while (true) {
					List<Person> queue = new LinkedList<>();

					synchronized (personIterator) {
						while (personIterator.hasNext() && queue.size() < chunkSize) {
							queue.add(personIterator.next());
						}
					}

					if (queue.size() == 0) {
						return;
					}

					for (Person person : queue) {
						for (MATSimAssignmentProblem problem : solver.createProblems(person.getSelectedPlan())) {
							adapter.accept(solver.solveProblem(problem));
						}

						processedNumberOfPersons.incrementAndGet();
					}
				}
			}));
		}

		threads.forEach(Thread::start);

		double startTime = 1e-9 * System.nanoTime();
		double estimationInterval = 10.0;

		Thread infoThread = new Thread(() -> {
			long lastEstimationCount = 0;
			double lastEstimationTime = startTime;
			double estimatedRate = 1.0;

			do {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return;
				}

				double currentTime = 1e-9 * System.nanoTime();
				long currentlyProcessedNumberOfPersons = processedNumberOfPersons.get();

				if (lastEstimationTime + estimationInterval <= currentTime) {
					double deltaTime = currentTime - lastEstimationTime;
					long deltaCount = currentlyProcessedNumberOfPersons - lastEstimationCount;
					estimatedRate = deltaCount / deltaTime;

					lastEstimationCount = currentlyProcessedNumberOfPersons;
					lastEstimationTime = currentTime;
				}

				double expectedTime = Math
						.ceil((totalNumberOfPersons - currentlyProcessedNumberOfPersons) / estimatedRate);

				System.out.println(
						String.format("Location assignment: %d/%d (%.2f%%), ETA: %s", currentlyProcessedNumberOfPersons,
								totalNumberOfPersons, 100.0 * currentlyProcessedNumberOfPersons / totalNumberOfPersons,
								Time.writeTime(expectedTime)));
			} while (processedNumberOfPersons.get() < totalNumberOfPersons);
		});
		infoThread.start();

		for (Thread thread : threads) {
			thread.join();
		}

		infoThread.join();

		/*
		 * scenario.getPopulation().getPersons().values().stream().parallel().map(Person
		 * ::getSelectedPlan)
		 * .map(solver::createProblems).flatMap(Collection::stream).map(solver::
		 * solveProblem) .map(zurichStatistics::process).forEach(new
		 * LocationAssignmentPlanAdapter());
		 */
	}
}
