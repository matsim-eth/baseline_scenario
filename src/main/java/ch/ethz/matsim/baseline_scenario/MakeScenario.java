package ch.ethz.matsim.baseline_scenario;

import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import ch.ethz.ivt.matsim.playgrounds.sebhoerl.locations.RunParallelSampler;
import ch.ethz.ivt.matsim.playgrounds.sebhoerl.utils.ShiftTimes;
import ch.ethz.matsim.baseline_scenario.utils.FixFacilityActivityTypes;
import ch.ethz.matsim.baseline_scenario.utils.FixShopActivities;
import ch.ethz.matsim.baseline_scenario.utils.MergeSecondaryFacilities;
import ch.ethz.matsim.baseline_scenario.utils.RemoveInvalidPlans;
import ch.ethz.matsim.baseline_scenario.utils.TypicalDurationForActivityTypes;
import ch.ethz.matsim.baseline_scenario.utils.routing.BestResponseCarRouting;

public class MakeScenario {
	static public void main(String args[]) throws Exception {
		int numberOfThreads = Integer.parseInt(args[0]);
		double scenarioScale = Double.parseDouble(args[1]);

		Random random = new Random(0);

		// Input is Kirill's population

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile("population.xml.gz");
		new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes())
				.readFile("population_attributes.xml.gz");
		new MatsimFacilitiesReader(scenario).readFile("facilities.xml.gz");
		new MatsimNetworkReader(scenario.getNetwork()).readFile("network.xml.gz");

		// GENERAL PREPARATION AND FIXING

		// Clean network
		new NetworkCleaner().run(scenario.getNetwork());

		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setLength(Math.max(1.0, link.getLength()));
		}

		// Load secondary facilities (pmb)
		new MergeSecondaryFacilities(random, "shop", "ShoppingFacilitiesFull.csv", scenarioScale, scenario.getNetwork())
				.run(scenario.getActivityFacilities());
		new MergeSecondaryFacilities(random, "leisure", "LeisureFacilitiesFull.csv", scenarioScale,
				scenario.getNetwork()).run(scenario.getActivityFacilities());

		// Add missing activity types to facilities (escort, ...) and remove opening
		// times from "home"
		new FixFacilityActivityTypes().run(scenario.getActivityFacilities());

		// Some shop activities are named "shopping" ... change that!
		new FixShopActivities().apply(scenario.getPopulation());

		// Remove invalid plans (not starting or ending with "home", zero durations)
		new RemoveInvalidPlans().apply(scenario.getPopulation());

		// DEPATURE TIMES

		// Dilute departure times
		new ShiftTimes(1800.0, random).apply(scenario.getPopulation());

		// LOCATION CHOICE

		Set<Id<Person>> failedIds = RunParallelSampler.run(numberOfThreads, "microcensus.csv", scenario.getPopulation(),
				scenario.getActivityFacilities());
		failedIds.forEach(id -> scenario.getPopulation().getPersons().remove(id));

		// SCORING

		// Adjust activities for typical durations
		new TypicalDurationForActivityTypes().run(scenario.getPopulation(), scenario.getActivityFacilities());

		// PREPARE FOR RUNNING

		// Do best response routing with free-flow travel times
		//new BestResponseCarRouting(numberOfThreads, scenario.getNetwork()).run(scenario.getPopulation());

		// OUTPUT

		new PopulationWriter(scenario.getPopulation()).write("output_population.xml.gz");
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes())
				.writeFile("output_population_attributes.xml.gz");
		new FacilitiesWriter(scenario.getActivityFacilities()).write("output_facilities.xml.gz");
		new NetworkWriter(scenario.getNetwork()).write("output_network.xml.gz");
	}
}
