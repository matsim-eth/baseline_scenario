package ch.ethz.matsim.baseline_scenario;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ch.ethz.ivt.matsim.playgrounds.sebhoerl.locations.RunParallelSampler;
import ch.ethz.ivt.matsim.playgrounds.sebhoerl.utils.Downsample;
import ch.ethz.ivt.matsim.playgrounds.sebhoerl.utils.ShiftTimes;
import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.DailyReferenceCountsReader;
import ch.ethz.matsim.baseline_scenario.config.BaselineConfig;
import ch.ethz.matsim.baseline_scenario.utils.AdaptConfig;
import ch.ethz.matsim.baseline_scenario.utils.AttributeCleaner;
import ch.ethz.matsim.baseline_scenario.utils.FixFacilityActivityTypes;
import ch.ethz.matsim.baseline_scenario.utils.FixLinkIds;
import ch.ethz.matsim.baseline_scenario.utils.FixShopActivities;
import ch.ethz.matsim.baseline_scenario.utils.HomeFacilitiesCleaner;
import ch.ethz.matsim.baseline_scenario.utils.HouseholdAttributeCleaner;
import ch.ethz.matsim.baseline_scenario.utils.HouseholdsCleaner;
import ch.ethz.matsim.baseline_scenario.utils.MergeSecondaryFacilities;
import ch.ethz.matsim.baseline_scenario.utils.RemoveInvalidPlans;
import ch.ethz.matsim.baseline_scenario.utils.TypicalDurationForActivityTypes;
import ch.ethz.matsim.baseline_scenario.utils.UnselectedPlanRemoval;
import ch.ethz.matsim.baseline_scenario.utils.consistency.MD5Collector;
import ch.ethz.matsim.baseline_scenario.utils.counts.TrafficCountPlanSelector;
import ch.ethz.matsim.baseline_scenario.utils.routing.BestResponseCarRouting;

public class MakeScenario {
	static public void main(String args[]) throws Exception {
		ObjectMapper json = new ObjectMapper();
		json.enable(SerializationFeature.INDENT_OUTPUT);

		BaselineConfig baselineConfig = json.readValue(new File(args[0]), BaselineConfig.class);

		File inputPath = new File(baselineConfig.inputPath);
		File outputPath = new File(baselineConfig.outputPath);

		if (!inputPath.exists()) {
			throw new IllegalArgumentException("Input path does not exist: " + inputPath);
		}

		outputPath.mkdirs();

		MD5Collector inputFilesCollector = new MD5Collector(inputPath);
		MD5Collector outputFilesCollector = new MD5Collector(outputPath);

		// TODO: Move down
		Config config = new AdaptConfig().run(baselineConfig.outputScenarioScale, baselineConfig.prefix);
		new ConfigWriter(config).write(new File(outputPath, baselineConfig.prefix + "config.xml").getPath());
		outputFilesCollector.add(baselineConfig.prefix + "config.xml");

		Random random = new Random(0);

		// Input is Kirill's population

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(new File(inputPath, "population.xml.gz").getPath());
		new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes())
				.readFile(new File(inputPath, "population_attributes.xml.gz").getPath());
		new MatsimFacilitiesReader(scenario).readFile(new File(inputPath, "facilities.xml.gz").getPath());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(new File(inputPath, "network.xml.gz").getPath());
		Collection<DailyCountItem> countItems = new DailyReferenceCountsReader(scenario.getNetwork())
				.read(new File(inputPath, "daily_counts.csv").getPath());
		new HouseholdsReaderV10(scenario.getHouseholds()).readFile(new File(inputPath, "households.xml.gz").getPath());
		new ObjectAttributesXmlReader(scenario.getHouseholds().getHouseholdAttributes())
				.readFile(new File(inputPath, "household_attributes.xml.gz").getPath());

		inputFilesCollector.add("population.xml.gz");
		inputFilesCollector.add("population_attributes.xml.gz");
		inputFilesCollector.add("facilities.xml.gz");
		inputFilesCollector.add("network.xml.gz");
		inputFilesCollector.add("daily_counts.csv");
		inputFilesCollector.add("households.xml.gz");
		inputFilesCollector.add("household_attributes.xml.gz");

		// Debug: Scale down for testing purposes already in the beginning (or for 25%
		// scenario)
		new Downsample(baselineConfig.inputDownsampling, random).run(scenario.getPopulation());

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
		new MergeSecondaryFacilities(random, "shop", new File(inputPath, "ShoppingFacilitiesFull.csv").getPath(), 1.0,
				scenario.getNetwork()).run(scenario.getActivityFacilities());
		new MergeSecondaryFacilities(random, "leisure", new File(inputPath, "LeisureFacilitiesFull.csv").getPath(), 1.0,
				scenario.getNetwork()).run(scenario.getActivityFacilities());

		inputFilesCollector.add("ShoppingFacilitiesFull.csv");
		inputFilesCollector.add("LeisureFacilitiesFull.csv");

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

		inputFilesCollector.add("microcensus.csv");
		Set<Id<Person>> failedIds = RunParallelSampler.run(baselineConfig.numberOfThreads,
				new File(inputPath, "microcensus.csv").getPath(), scenario.getPopulation(),
				scenario.getActivityFacilities(), baselineConfig.performIterativeLocationChoice ? 20 : 1);
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
		new BestResponseCarRouting(baselineConfig.numberOfThreads, scenario.getNetwork()).run(scenario.getPopulation());

		if (baselineConfig.performIterativeLocationChoice) {
			// Select plans to fit counts
			new TrafficCountPlanSelector(scenario.getNetwork(), countItems, baselineConfig.outputScenarioScale, 0.01,
					baselineConfig.numberOfThreads, new File(outputPath, "counts_locchoice.txt").getPath(), 20)
							.run(scenario.getPopulation());
			new UnselectedPlanRemoval().run(scenario.getPopulation());
		}

		// Here we get some nice pre-initialized routes for free, because
		// the TrafficCountPlanSelector already estimates them using BPR

		// Clean attributes
		personAttributeNames.addAll(Arrays.asList("mz_id", "season_ticket"));
		AttributeCleaner<Person> personAttributesCleaner = new AttributeCleaner<>(personAttributeNames);
		ObjectAttributes cleanedPersonAttributes = personAttributesCleaner
				.run(scenario.getPopulation().getPersons().values(), scenario.getPopulation().getPersonAttributes());

		new HouseholdsCleaner(scenario.getPopulation().getPersons().keySet()).run(scenario.getHouseholds());
		new HomeFacilitiesCleaner(scenario.getHouseholds().getHouseholds().keySet(),
				scenario.getPopulation().getPersons().values()).run(scenario.getActivityFacilities());

		// TODO: Here we cannot use the generic AttributeCleaner for Households, because
		// they do not implement the Identifiable interface. Need to fix this in MATSim
		// core.

		Set<String> householdAttributeNames = new HashSet<>(Arrays.asList("numberOfPrivateCars", "bikeAvailability"));
		HouseholdAttributeCleaner householdAttributesCleaner = new HouseholdAttributeCleaner(householdAttributeNames);
		ObjectAttributes cleanedHouseholdAttributes = householdAttributesCleaner.run(
				scenario.getHouseholds().getHouseholds().values(), scenario.getHouseholds().getHouseholdAttributes());

		// OUTPUT

		new PopulationWriter(scenario.getPopulation())
				.write(new File(outputPath, baselineConfig.prefix + "population.xml.gz").getPath());
		new ObjectAttributesXmlWriter(cleanedPersonAttributes)
				.writeFile(new File(outputPath, baselineConfig.prefix + "population_attributes.xml.gz").getPath());
		new HouseholdsWriterV10(scenario.getHouseholds())
				.writeFile(new File(outputPath, baselineConfig.prefix + "households.xml.gz").getPath());
		new ObjectAttributesXmlWriter(cleanedHouseholdAttributes)
				.writeFile(new File(outputPath, baselineConfig.prefix + "household_attributes.xml.gz").getPath());
		new FacilitiesWriter(scenario.getActivityFacilities())
				.write(new File(outputPath, baselineConfig.prefix + "facilities.xml.gz").getPath());
		new NetworkWriter(scenario.getNetwork())
				.write(new File(outputPath, baselineConfig.prefix + "network.xml.gz").getPath());
		json.writeValue(new File(outputPath, baselineConfig.prefix + "make_config.json"), baselineConfig);
		inputFilesCollector.write(new File(outputPath, baselineConfig.prefix + "input.md5"));

		outputFilesCollector.add(baselineConfig.prefix + "population.xml.gz");
		outputFilesCollector.add(baselineConfig.prefix + "population_attributes.xml.gz");
		outputFilesCollector.add(baselineConfig.prefix + "facilities.xml.gz");
		outputFilesCollector.add(baselineConfig.prefix + "network.xml.gz");
		outputFilesCollector.add(baselineConfig.prefix + "households.xml.gz");
		outputFilesCollector.add(baselineConfig.prefix + "household_attributes.xml.gz");
		outputFilesCollector.add(baselineConfig.prefix + "make_config.json");
		outputFilesCollector.add(baselineConfig.prefix + "input.md5");

		outputFilesCollector.write(new File(outputPath, baselineConfig.prefix + "output.md5"));
	}
}
