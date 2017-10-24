package ch.ethz.matsim.baseline_scenario.utils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.DailyReferenceCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.HourlyReferenceCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.utils.HourlyCountsAggregator;
import ch.ethz.matsim.baseline_scenario.analysis.counts.utils.IndexBuilder;

public class LocationPlanChoice {
	final private Random random;
	final private LinkedHashMap<Id<Link>, DailyCountItem> counts;

	public LocationPlanChoice(Random random, Collection<DailyCountItem> counts) {
		this.random = random;
		this.counts = new LinkedHashMap<>(IndexBuilder.buildDailyIndex(counts));
	}

	static public void main(String[] args) throws IOException {
		String populationInput = "/home/sebastian/baseline_scenario/data/output_population.xml.gz";
		String networkInput = "/home/sebastian/baseline_scenario/data/output_network.xml.gz";
		String astraCountsInput = "/home/sebastian/temp/streetCounts_ASTRA.csv";
		String ktzhCountsInput = "/home/sebastian/temp/streetCounts_KtZH.csv";
		
		Random random = new Random();
		
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkInput);
		
		Collection<DailyCountItem> counts = new LinkedList<>();
		
		counts.addAll(new DailyReferenceCountsReader(network).read(ktzhCountsInput));
		counts.addAll(new HourlyCountsAggregator().aggregate(new HourlyReferenceCountsReader(network).read(astraCountsInput)));
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationInput);
		
		new LocationPlanChoice(random, counts).run(scenario.getPopulation());
	}

	public void run(Population population) throws IOException {
		double factor = 0.01;
		
		int N = population.getPersons().size();
		int M = population.getPersons().values().iterator().next().getPlans().size();
		int K = counts.size();

		int[] referenceCounts = new int[K];

		{
			int k = 0;

			for (DailyCountItem item : counts.values()) {
				referenceCounts[k] = item.reference;
				k++;
			}
		}

		int[][][] routes = new int[N][M][K];

		{
			int i = 0;

			for (Person person : population.getPersons().values()) {
				int j = 0;

				for (Plan plan : person.getPlans()) {
					int k = 0;

					for (Id<Link> referenceId : counts.keySet()) {
						routes[i][j][k] = 0;

						for (Leg leg : TripStructureUtils.getLegs(plan)) {
							if (leg.getMode().equals("car")) {
								NetworkRoute route = (NetworkRoute) leg.getRoute();

								if (route.getStartLinkId().equals(referenceId)
										|| route.getEndLinkId().equals(referenceId)
										|| route.getLinkIds().contains(referenceId)) {
									routes[i][j][k]++;
								}
							}
						}

						k++;
					}

					j++;
				}

				i++;
			}
		}
		
		List<Integer> relevantIndices = new LinkedList<>();
		
		{
			for (int i = 0; i < N; i++) {
				boolean relevant = false;
				
				for (int j = 0; j < M; j++) {
					for (int k = 0; k < K; k++) {
						if (routes[i][j][k] > 0) {
							relevant = true;
							break;
						}
					}
					
					if (relevant) {
						break;
					}
				}
				
				if (relevant) {
					relevantIndices.add(i);
				}
			}
		}
		
		int[] selection = new int[N];
		double objective = getObjective(N, K, routes, referenceCounts, selection, factor);
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/sebastian/temp/dists.txt")));
		int[] current = new int[K];
		
		for (int k = 0; k < K; k++) writer.write(referenceCounts[k] + " ");
		writer.write("0 0\n");	
		writer.flush();
		
		for (int iteration = 0; iteration < 100; iteration++) {
			double referenceTotal = 0.0;
			double selectionTotal = 0.0;
			
			for (int i = 0; i < N; i++) {
				for (int k = 0; k < K; k++) {
					current[k] += routes[i][selection[i]][k];
				}
			}
			
			for (int k = 0; k < K; k++) {
				referenceTotal += referenceCounts[k];
				selectionTotal += current[k];
			}
			
			for (int k = 0; k < K; k++) writer.write(current[k] + " ");
			writer.write(referenceTotal + " " + selectionTotal);
			writer.write("\n");	
			writer.flush();
			
			int u = 0;
			
			for (int i : relevantIndices) {
				for (int j = 0; j < M; j++) {
					int cached = selection[i];
					selection[i] = j;
					
					double updatedObjective = getObjective(N, K, routes, referenceCounts, selection, factor);
					
					if (updatedObjective < objective) {
						objective = updatedObjective;
					} else {
						selection[i] = cached;
					}
				}
				
				if (u % 10 == 0) System.out.println("it " + iteration + "; " + u + "/" + relevantIndices.size() + ": " + objective);
				u++;
			}
			
			
		}
		
		return;
		
		
		
		/*
		for (int i = 0; i < N; i++) {
			double personBestObjective = Double.POSITIVE_INFINITY;
			int personBestIndex = -1;
			
			for (int j = 0; j < M; j++) {
				boolean[] currentRoute = routes[i][j];
				
				
			}
		}
		
		
		

		int best_selector[] = new int[N];
		double best_objective = Double.POSITIVE_INFINITY;

		double phi = 1.0;

		for (int iteration = 0; iteration < 100000; iteration++) {
			int selector[] = Arrays.copyOf(best_selector, best_selector.length);
			selector[random.nextInt(N)] = random.nextInt(M);

			int[] current_counts = new int[K];

			for (int i = 0; i < N; i++) {
				boolean[] selected_route = routes[i][selector[i]];

				for (int k = 0; k < K; k++) {
					if (selected_route[k]) {
						current_counts[k]++;
					}
				}
			}

			double objective = 0.0;
			double counts_sum = 0.0;

			for (int k = 0; k < K; k++) {
				objective += Math.pow(phi * current_counts[k] - reference_counts[k], 2.0);
				counts_sum += current_counts[k];
			}

			objective = Math.sqrt(objective);

			if (objective <= best_objective) {
				best_objective = objective;
				best_selector = selector;
				
				//phi = phi * 0.9 + 0.1 * (reference_sum / counts_sum);
			}

			System.out.println(best_objective + " " + phi);
		}*/
	}
	
	private double getObjective(int N, int K, int[][][] routes, int[] referenceCounts, int[] selection, double factor) {
		int[] currentCounts = new int[K];

		for (int i = 0; i < N; i++) {
			int[] selected_route = routes[i][selection[i]];

			for (int k = 0; k < K; k++) {
				currentCounts[k] += selected_route[k];
			}
		}
		
		
		double objective = 0.0;

		for (int k = 0; k < K; k++) {
			/*double diff = Math.abs(phi * currentCounts[k] - referenceCounts[k]);
			
			if (diff > objective) {
				objective = diff;
			}*/
			
			//objective += Math.pow(phi * currentCounts[k] - referenceCounts[k], 2.0);
			objective += Math.abs(currentCounts[k] / factor - referenceCounts[k]);
		}
		
		return objective; //Math.sqrt(objective);
	}
}
