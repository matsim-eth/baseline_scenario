package ch.ethz.matsim.baseline_scenario.location_assignment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import ch.ethz.matsim.location_assignment.algorithms.DistanceSampler;

public class ZurichDistanceSamplerFactory {
	final private Random random;

	final private Map<String, List<Double>> quantiles = new HashMap<>();
	final private Map<String, List<List<Double>>> distributions = new HashMap<>();

	public ZurichDistanceSamplerFactory(Random random) {
		this.random = random;
	}

	public DistanceSampler createDistanceSampler(String mode, double travelTime) {
		return new DistanceSampler() {
			@Override
			public double sample() {
				List<Double> samples = distributions.get(mode).get(getQuantile(mode, travelTime));
				return samples.get(random.nextInt(samples.size()));
			}
		};
	}

	private int getQuantile(String mode, double travelTime) {
		List<Double> modeQuantiles = quantiles.get(mode);

		int i = 0;

		while (travelTime > modeQuantiles.get(i) && i < modeQuantiles.size() - 1) {
			i++;
		}

		return i;
	}

	public void load(String quantilesPath, String distributionsPath) throws IOException {
		BufferedReader reader;
		String line = null;

		reader = new BufferedReader(new InputStreamReader(new FileInputStream(quantilesPath)));

		while ((line = reader.readLine()) != null) {
			List<String> row = Arrays.asList(line.split(";"));
			List<Double> bounds = row.subList(1, row.size()).stream().map(Double::parseDouble)
					.collect(Collectors.toList());
			quantiles.put(row.get(0), bounds);
		}

		reader.close();

		for (Map.Entry<String, List<Double>> entry : quantiles.entrySet()) {
			List<List<Double>> modeDistributions = new LinkedList<>();

			for (int i = 0; i < entry.getValue().size(); i++) {
				modeDistributions.add(new LinkedList<>());
			}

			distributions.put(entry.getKey(), modeDistributions);
		}

		reader = new BufferedReader(new InputStreamReader(new FileInputStream(distributionsPath)));

		while ((line = reader.readLine()) != null) {
			List<String> row = Arrays.asList(line.split(";"));
			List<Double> samples = row.subList(2, row.size()).stream().map(Double::parseDouble)
					.collect(Collectors.toList());
			distributions.get(row.get(0)).get(Integer.valueOf(row.get(1))).addAll(samples);
		}

		reader.close();
	}
	
	public void duplicateDistribution(String sourceDistribution, String targetDistribution) {
		distributions.put(targetDistribution, distributions.get(sourceDistribution));
		quantiles.put(targetDistribution, quantiles.get(sourceDistribution));
	}
}
