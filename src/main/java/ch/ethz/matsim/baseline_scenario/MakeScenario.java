package ch.ethz.matsim.baseline_scenario;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import ch.ethz.ivt.matsim.playgrounds.sebhoerl.locations.RunParallelSampler;
import ch.ethz.ivt.matsim.playgrounds.sebhoerl.utils.Downsample;
import ch.ethz.ivt.matsim.playgrounds.sebhoerl.utils.ShiftTimes;
import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.DailyReferenceCountsReader;
import ch.ethz.matsim.baseline_scenario.utils.AdaptConfig;
import ch.ethz.matsim.baseline_scenario.utils.AttributeCleaner;
import ch.ethz.matsim.baseline_scenario.utils.FixFacilityActivityTypes;
import ch.ethz.matsim.baseline_scenario.utils.FixLinkIds;
import ch.ethz.matsim.baseline_scenario.utils.FixShopActivities;
import ch.ethz.matsim.baseline_scenario.utils.MergeSecondaryFacilities;
import ch.ethz.matsim.baseline_scenario.utils.RemoveInvalidPlans;
import ch.ethz.matsim.baseline_scenario.utils.TypicalDurationForActivityTypes;
import ch.ethz.matsim.baseline_scenario.utils.UnselectedPlanRemoval;
import ch.ethz.matsim.baseline_scenario.utils.counts.TrafficCountPlanSelector;
import ch.ethz.matsim.baseline_scenario.utils.routing.BestResponseCarRouting;

public class MakeScenario {
	static public void main(String args[]) throws Exception {
		double scenarioScale = Double.parseDouble(args[0]);
		int numberOfThreads = Integer.parseInt(args[1]);
		double downsampling = args.length > 2 ? Double.parseDouble(args[2]) : 1.0;

		// TODO: Move down
		Config config = new AdaptConfig().run(scenarioScale);
		new ConfigWriter(config).write("output_config.xml");

		Random random = new Random(0);

		// Input is Kirill's population

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile("population.xml.gz");
		new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes())
				.readFile("population_attributes.xml.gz");
		new MatsimFacilitiesReader(scenario).readFile("facilities.xml.gz");
		new MatsimNetworkReader(scenario.getNetwork()).readFile("network.xml.gz");
		Collection<DailyCountItem> countItems = new DailyReferenceCountsReader(scenario.getNetwork())
				.read("daily_counts.csv");

		// Debug: Scale down for testing purposes already in the beginning (or for 25%
		// scenario)
		new Downsample(downsampling, random).run(scenario.getPopulation());

		// GENERAL PREPARATION AND FIXING

		// Clean network
		Set<Id<Link>> remove = scenario.getNetwork().getLinks().values().stream()
				.filter(l -> !l.getAllowedModes().contains("car")).map(l -> l.getId()).collect(Collectors.toSet());
		remove.forEach(id -> scenario.getNetwork().removeLink(id));

		new NetworkCleaner().run(scenario.getNetwork());

		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setLength(Math.max(1.0, link.getLength()));
		}

		// Set link ids for activities and facilities
		new FixLinkIds(scenario.getNetwork()).run(scenario.getActivityFacilities(), scenario.getPopulation());

		// Load secondary facilities (pmb)
		new MergeSecondaryFacilities(random, "shop", "ShoppingFacilitiesFull.csv", 1.0, scenario.getNetwork())
				.run(scenario.getActivityFacilities());
		new MergeSecondaryFacilities(random, "leisure", "LeisureFacilitiesFull.csv", 1.0, scenario.getNetwork())
				.run(scenario.getActivityFacilities());

		// Add missing activity types to facilities (escort, ...) and remove opening
		// times from "home"
		new FixFacilityActivityTypes().run(scenario.getActivityFacilities());

		// Some shop activities are named "shopping" ... change that!
		new FixShopActivities().apply(scenario.getPopulation());

		// Remove invalid plans (not starting or ending with "home", zero durations)
		new RemoveInvalidPlans().apply(scenario.getPopulation());

		// DEPATURE TIMES

		// Dilute departure times
		new ShiftTimes(1800.0, random, false).apply(scenario.getPopulation());

		// LOCATION CHOICE

		Set<Id<Person>> failedIds = RunParallelSampler.run(numberOfThreads, "microcensus.csv", scenario.getPopulation(),
				scenario.getActivityFacilities(), 20);
		failedIds.forEach(id -> scenario.getPopulation().getPersons().remove(id));

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;

						if (leg.getRoute() != null && leg.getRoute().getRouteType().equals("DebugInformation")) {
							leg.setRoute(null);
						}
					}
				}
			}
		}

		// SCORING

		// Adjust activities for typical durations
		Set<String> personAttributeNames = new HashSet<>();
		new TypicalDurationForActivityTypes().run(scenario.getPopulation(), scenario.getActivityFacilities(),
				personAttributeNames);

		// PREPARE FOR RUNNING

		// Do best response routing with free-flow travel times
		new BestResponseCarRouting(numberOfThreads, scenario.getNetwork()).run(scenario.getPopulation());

		// Select plans to fit counts
		new TrafficCountPlanSelector(scenario.getNetwork(), countItems, scenarioScale, 0.01, numberOfThreads,
				"counts_locchoice.txt", 20).run(scenario.getPopulation());
		new UnselectedPlanRemoval().run(scenario.getPopulation());

		// Here we get some nice pre-initialized routes for free, because
		// the TrafficCountPlanSelector already estimates them using BPR

		// Clean attributes
		personAttributeNames.addAll(Arrays.asList("mz_id", "season_ticket"));
		AttributeCleaner<Person> cleaner = new AttributeCleaner<>(personAttributeNames);
		ObjectAttributes cleanedPersonAttributes = cleaner.run(scenario.getPopulation().getPersons().values(),
				scenario.getPopulation().getPersonAttributes());

		// OUTPUT

		new PopulationWriter(scenario.getPopulation()).write("output_population.xml.gz");
		new ObjectAttributesXmlWriter(cleanedPersonAttributes).writeFile("output_population_attributes.xml.gz");
		new FacilitiesWriter(scenario.getActivityFacilities()).write("output_facilities.xml.gz");
		new NetworkWriter(scenario.getNetwork()).write("output_network.xml.gz");
	}
}
