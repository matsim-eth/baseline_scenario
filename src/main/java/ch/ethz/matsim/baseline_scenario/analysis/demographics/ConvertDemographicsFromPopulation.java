package ch.ethz.matsim.baseline_scenario.analysis.demographics;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class ConvertDemographicsFromPopulation {
	public static void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new PopulationReader(scenario).readFile(args[0]);
		new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).readFile(args[1]);
		new HouseholdsReaderV10(scenario.getHouseholds()).readFile(args[2]);
		new ObjectAttributesXmlReader(scenario.getHouseholds().getHouseholdAttributes()).readFile(args[3]);

		Map<Id<Person>, Id<Household>> householdMap = new HashMap<>();

		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			household.getMemberIds().forEach(id -> householdMap.put(id, household.getId()));
		}

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[4])));
		writer.write(String.join(";",
				new String[] { "person_id", "age", "has_ga", "has_halbtax", "has_zvv", "hh_income", "hh_cars" })
				+ "\n");
		writer.flush();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Id<Person> personId = person.getId();

			String seasonTicket = (String) scenario.getPopulation().getPersonAttributes()
					.getAttribute(person.getId().toString(), "season_ticket");

			boolean hasGA = false;
			boolean hasHalbtax = false;
			boolean hasZVV = false;

			if (seasonTicket == null) {
				System.err.println("No seasonTicket for " + personId);
			} else {
				hasGA = seasonTicket.contains("General");
				hasHalbtax = seasonTicket.contains("Halbtax");
				hasZVV = seasonTicket.contains("Verbund");
			}

			int age = PersonUtils.getAge(person);

			Household household = scenario.getHouseholds().getHouseholds().get(householdMap.get(person.getId()));

			double householdIncome = -1.0;
			int numberOfHouseholdCars = -1;

			if (household == null) {
				System.err.println("No household for " + personId);
			} else {
				householdIncome = household.getIncome().getIncome();
				numberOfHouseholdCars = Integer.parseInt((String) scenario.getHouseholds().getHouseholdAttributes()
						.getAttribute(household.getId().toString(), "numberOfPrivateCars"));
			}

			writer.write(String.join(";",
					new String[] { personId.toString(), String.valueOf(age), String.valueOf(hasGA),
							String.valueOf(hasHalbtax), String.valueOf(hasZVV), String.valueOf(householdIncome),
							String.valueOf(numberOfHouseholdCars) })
					+ "\n");
			writer.flush();
		}

		writer.close();
	}
}
