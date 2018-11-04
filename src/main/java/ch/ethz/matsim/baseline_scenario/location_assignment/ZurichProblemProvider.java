package ch.ethz.matsim.baseline_scenario.location_assignment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;

import ch.ethz.matsim.location_assignment.algorithms.DistanceSampler;
import ch.ethz.matsim.location_assignment.algorithms.discretizer.Discretizer;
import ch.ethz.matsim.location_assignment.matsim.MATSimAssignmentProblem;
import ch.ethz.matsim.location_assignment.matsim.discretizer.FacilityTypeDiscretizerFactory;
import ch.ethz.matsim.location_assignment.matsim.setup.MATSimDiscretizationThresholdProvider;
import ch.ethz.matsim.location_assignment.matsim.setup.MATSimDiscretizerProvider;
import ch.ethz.matsim.location_assignment.matsim.setup.MATSimDistanceSamplerProvider;

public class ZurichProblemProvider
		implements MATSimDistanceSamplerProvider, MATSimDiscretizationThresholdProvider, MATSimDiscretizerProvider {
	final private ZurichDistanceSamplerFactory distanceSamplerFactory;
	final private FacilityTypeDiscretizerFactory discretizerFactory;
	final private Map<String, Double> discretizationThresholds;

	public ZurichProblemProvider(ZurichDistanceSamplerFactory distanceSamplerFactory,
			FacilityTypeDiscretizerFactory discretizerFactory, Map<String, Double> discretizationThresholds) {
		this.distanceSamplerFactory = distanceSamplerFactory;
		this.discretizerFactory = discretizerFactory;
		this.discretizationThresholds = discretizationThresholds;
	}

	@Override
	public List<Discretizer> getDiscretizers(MATSimAssignmentProblem problem) {
		return problem.getChainActivities().stream().map(Activity::getType).map(discretizerFactory::createDiscretizer)
				.collect(Collectors.toList());
	}

	@Override
	public List<Double> getDiscretizationThresholds(MATSimAssignmentProblem problem) {
		return problem.getAllLegs().stream().map(Leg::getMode).map(discretizationThresholds::get)
				.collect(Collectors.toList());
	}

	@Override
	public List<DistanceSampler> getDistanceSamplers(MATSimAssignmentProblem problem) {
		return problem.getAllLegs().stream().map(leg -> {
			double duration = PopulationUtils.getNextActivity(problem.getPlan(), leg).getStartTime()
					- PopulationUtils.getPreviousActivity(problem.getPlan(), leg).getEndTime();
			return distanceSamplerFactory.createDistanceSampler(leg.getMode(), duration);
		}).collect(Collectors.toList());
	}
}
