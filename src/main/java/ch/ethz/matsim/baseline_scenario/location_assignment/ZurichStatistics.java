package ch.ethz.matsim.baseline_scenario.location_assignment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.location_assignment.algorithms.ThresholdObjective;
import ch.ethz.matsim.location_assignment.assignment.LocationAssignmentResult;
import ch.ethz.matsim.location_assignment.matsim.MATSimAssignmentProblem;
import ch.ethz.matsim.location_assignment.matsim.solver.MATSimSolverResult;

public class ZurichStatistics {
	private long numberOfProblems = 0;
	private long numberOfProblemTrips = 0;
	private long numberOfConvergedProblemTrips = 0;
	private long numberOfConinuousConvergedProblems = 0;
	private long numberOfConvergedProblems = 0;

	private Set<Id<Person>> processedPersons = new HashSet<>();
	private long numberOfPersons;

	private Map<String, DescriptiveStatistics> errorStatistics = new HashMap<>();
	private Map<String, DescriptiveStatistics> absoluteErrorStatistics = new HashMap<>();
	private Map<String, DescriptiveStatistics> excessErrorStatistics = new HashMap<>();

	final private Optional<BufferedWriter> writer;

	public ZurichStatistics(long numberOfPersons) {
		this(numberOfPersons, null);
	}

	public ZurichStatistics(long numberOfPersons, Optional<OutputStream> output) {
		this.numberOfPersons = numberOfPersons;
		this.writer = output.map(OutputStreamWriter::new).map(BufferedWriter::new);

		for (String mode : Arrays.asList("car", "pt", "bike", "walk")) {
			errorStatistics.put(mode, new DescriptiveStatistics());
			absoluteErrorStatistics.put(mode, new DescriptiveStatistics());
			excessErrorStatistics.put(mode, new DescriptiveStatistics());
		}

		writeHeader();
	}

	public MATSimSolverResult process(MATSimSolverResult result) {
		process(result.getProblem(), result.getResult());
		return result;
	}

	private void writeHeader() {
		if (writer.isPresent()) {
			try {
				writer.get().write(String.join(";", new String[] { "person_id", "person_trip_id", "mode", "error",
						"absolute_error", "excess_error" }) + "\n");
				writer.get().flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void writeOutput(MATSimAssignmentProblem problem, LocationAssignmentResult result) {
		if (writer.isPresent()) {
			try {
				List<PlanElement> planElements = problem.getPlan().getPlanElements();
				List<Activity> activities = problem.getAllActivities();
				List<Leg> legs = problem.getAllLegs();

				List<Integer> tripIndices = activities.stream().map(planElements::indexOf).map(i -> i / 2)
						.collect(Collectors.toList());
				ThresholdObjective objective = (ThresholdObjective) result.getObjective();

				for (int i = 0; i < objective.getErrors().size(); i++) {
					writer.get()
							.write(String.join(";",
									new String[] { problem.getPlan().getPerson().getId().toString(),
											String.valueOf(tripIndices.get(i)), legs.get(i).getMode(),
											String.valueOf(objective.getErrors().get(i)),
											String.valueOf(objective.getAbsoluteErrors().get(i)),
											String.valueOf(objective.getExcessErrors().get(i)) })
									+ "\n");
					writer.get().flush();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	synchronized public void process(MATSimAssignmentProblem problem, LocationAssignmentResult result) {
		writeOutput(problem, result);
		processedPersons.add(problem.getPlan().getPerson().getId());

		List<String> modes = problem.getAllLegs().stream().map(Leg::getMode).collect(Collectors.toList());
		ThresholdObjective objective = (ThresholdObjective) result.getObjective();

		if (result.getFeasibleDistanceResult().isConverged()) {
			numberOfConinuousConvergedProblems++;
		}

		if (result.getObjective().isConverged()) {
			numberOfConvergedProblems++;
		}

		numberOfProblems++;
		numberOfProblemTrips += modes.size();

		for (int i = 0; i < modes.size(); i++) {
			errorStatistics.get(modes.get(i)).addValue(objective.getErrors().get(i));
			absoluteErrorStatistics.get(modes.get(i)).addValue(objective.getAbsoluteErrors().get(i));

			double excessDeviation = objective.getExcessErrors().get(i);

			if (excessDeviation > 0.0) {
				excessErrorStatistics.get(modes.get(i)).addValue(excessDeviation);
			} else {
				numberOfConvergedProblemTrips++;
			}
		}

		double convergenceRate = (double) numberOfConvergedProblems / (double) numberOfProblems;
		double continuousConvergenceRate = (double) numberOfConinuousConvergedProblems / (double) numberOfProblems;
		double progress = (double) processedPersons.size() / (double) numberOfPersons;
		double tripConvergenceRate = (double) numberOfConvergedProblemTrips / (double) numberOfProblemTrips;

		Map<String, DescriptiveStatistics> statistics = absoluteErrorStatistics;
		Function<DescriptiveStatistics, Double> describe = (DescriptiveStatistics s) -> s.getPercentile(90.0);

		String deviationOutput = String.format("car: %.2fm, pt: %.2fm, bike: %.2fm, walk: %.2fm",
				describe.apply(statistics.get("car")), describe.apply(statistics.get("pt")),
				describe.apply(statistics.get("bike")), describe.apply(statistics.get("walk")));

		System.out.println(
				String.format("%d/%d (%.2f%%), Relaxed: %.2f%%, Discrete: %.2f%%, Trips: %.2f%%, Deviation: %s",
						processedPersons.size(), numberOfPersons, progress * 100.0, continuousConvergenceRate * 100.0,
						convergenceRate * 100.0, tripConvergenceRate * 100.0, deviationOutput));
	}
}
