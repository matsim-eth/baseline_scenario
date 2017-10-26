package ch.ethz.matsim.baseline_scenario.utils.counts;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.utils.IndexBuilder;

public class CountFittingProblem {
	final private LinkedHashMap<Id<Link>, DailyCountItem> countItems;
	final private List<Person> persons;

	final private List<Integer> relevantIndices = new LinkedList<>();
	final private int routes[][][];

	final private int N;
	final private int M;
	final private int K;

	final int[] reference;

	final int[] selection;
	final int[] counts;
	double objective = Double.POSITIVE_INFINITY;

	final private double scaling;

	public CountFittingProblem(double scaling, Collection<DailyCountItem> counts, Collection<Person> persons) {
		this.scaling = scaling;
		this.countItems = new LinkedHashMap<>(IndexBuilder.buildDailyIndex(counts));
		this.persons = new LinkedList<>(persons);

		this.N = persons.size();
		this.M = persons.iterator().next().getPlans().size();
		this.K = countItems.size();

		this.reference = new int[K];
		this.counts = new int[K];

		int k = 0;
		for (DailyCountItem item : this.countItems.values()) {
			reference[k] = item.reference;
			k++;
		}

		this.routes = new int[N][M][K];
		this.selection = new int[N];
	}

	public void update(Collection<Person> active) {
		{
			int i = 0;

			for (Person person : persons) {
				if (!active.contains(person)) {
					i++;
					continue;
				}
				
				int j = 0;

				for (Plan plan : person.getPlans()) {
					int k = 0;

					if (person.getSelectedPlan().equals(plan)) {
						selection[i] = j;
					}

					for (Id<Link> referenceId : countItems.keySet()) {
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

		relevantIndices.clear();

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
		
		initializeObjective();
	}

	private void initializeObjective() {
		for (int k = 0; k < K; k++) {
			counts[k] = 0;
		}
		
		for (int i : relevantIndices) {
			int[] selectedRoute = routes[i][selection[i]];

			for (int k = 0; k < K; k++) {
				counts[k] += selectedRoute[k];
			}
		}

		double objective = 0.0;

		for (int k = 0; k < K; k++) {
			objective += Math.abs(counts[k] - reference[k] * scaling);
			//objective += Math.pow(counts[k] - reference[k] * scaling, 2.0);
			//objective = Math.max(objective, Math.abs(counts[k] - reference[k] * scaling));
		}

		this.objective = objective;
	}

	private double computeChangeObjective(int i, int j) {
		int[] previousRoute = routes[i][selection[i]];
		int[] nextRoute = routes[i][j];

		double temporaryObjective = 0.0;

		for (int k = 0; k < K; k++) {
			int newCount = counts[k] - previousRoute[k] + nextRoute[k];
			temporaryObjective += Math.abs(newCount - reference[k] * scaling);
			//temporaryObjective += Math.pow(newCount - reference[k] * scaling, 2.0);
			//temporaryObjective = Math.max(temporaryObjective, Math.abs(newCount - reference[k] * scaling));
		}

		return temporaryObjective;
	}
	
	private void applyChange(int i, int j, double updatedObjective) {
		int[] previousRoute = routes[i][selection[i]];
		int[] nextRoute = routes[i][j];
		
		for (int k = 0; k < K; k++) {
			counts[k] = counts[k] - previousRoute[k] + nextRoute[k];
		}
		
		selection[i] = j;
		objective = updatedObjective;
	}

	public void solve() {
		for (int i : relevantIndices) {
			for (int j = 0; j < M; j++) {
				double updatedObjective = computeChangeObjective(i, j);

				if (updatedObjective < objective) {
					applyChange(i,j, updatedObjective);
				}
			}

			persons.get(i).setSelectedPlan(persons.get(i).getPlans().get(selection[i]));
		}
	}
	
	public double getObjective() {
		return objective;
	}
	
	public int[] getCounts() {
		return counts;
	}
	
	public int[] getReference() {
		return reference;
	}
	
	public List<Person> getRelevantPersons() {
		List<Person> relevant = new LinkedList<>();
		for (int i : relevantIndices) relevant.add(persons.get(i));
		return relevant;
	}
}
