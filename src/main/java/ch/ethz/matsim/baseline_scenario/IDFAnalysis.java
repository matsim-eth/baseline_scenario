package ch.ethz.matsim.baseline_scenario;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.MathTransform;

import com.fasterxml.jackson.databind.ObjectMapper;

public class IDFAnalysis {
	static public void main(String[] args) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		MathTransform transform = CRS.findMathTransform(CRS.decode("EPSG:2154"), CRS.decode("EPSG:4326"));

		FeatureCollection featureCollection = new FeatureCollection();

		Feature feature = new Feature();
		feature.setGeometry(new LineString(new LngLatAlt(0.0, 0.0), new LngLatAlt(1.0, 1.0)));
		feature.setProperty("abc", 55);

		featureCollection.add(feature);

		System.out.println(new ObjectMapper().writeValueAsString(featureCollection));
		new ObjectMapper().writeValue(new File("output.geojson"), featureCollection);

		System.exit(1);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(args[0]);
		new HouseholdsReaderV10(scenario.getHouseholds()).readFile(args[1]);

		/*BufferedWriter activityWriter = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream("activities.csv")));
		BufferedWriter personWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("persons.csv")));
		BufferedWriter householdWriter = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream("households.csv")));*/
		
		FeatureCollection activityCollection = new FeatureCollection();
		FeatureCollection personCollection = new FeatureCollection();
		FeatureCollection householdCollection = new FeatureCollection();

		//activityWriter.write("purpose;x;y\n");
		//personWriter.write("home_x;home_y;work_x;work_y;education_x;education_y\n");
		//householdWriter.write("income;x;y\n");

		for (Person person : scenario.getPopulation().getPersons().values()) {
			LngLatAlt homeCoord = null;
			LngLatAlt workCoord = null;
			LngLatAlt educationCoord = null;

			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = (Activity) element;
					
					DirectPosition2D sourcePosition = new DirectPosition2D(activity.getCoord().getX(), activity.getCoord().getY());
					DirectPosition2D targetPosition = new DirectPosition2D();
					transform.transform(sourcePosition, targetPosition);
					LngLatAlt coordinate = new LngLatAlt(sourcePosition.getX(), sourcePosition.getY());

					if (!activity.getType().contains("interaction")) {
						/*activityWriter.write(String.join(";",
								new String[] { activity.getType(), String.valueOf(activity.getCoord().getX()),
										String.valueOf(activity.getCoord().getY()) })
								+ "\n");
						activityWriter.flush();*/
						

						
						Feature activityFeature = new Feature();
						activityFeature.setGeometry(new Point(coordinates));
						
					}

					if (activity.getType().equals("work")) {
						workCoord = activity.getCoord();
					}

					if (activity.getType().equals("education")) {
						educationCoord = activity.getCoord();
					}

					if (activity.getType().equals("home")) {
						homeCoord = activity.getCoord();
					}
				}
			}

			personWriter.write(String.valueOf(homeCoord.getX()) + ";");
			personWriter.write(String.valueOf(homeCoord.getY()) + ";");

			if (workCoord != null) {
				personWriter.write(String.valueOf(workCoord.getX()) + ";");
				personWriter.write(String.valueOf(workCoord.getX()) + ";");
			} else {
				personWriter.write("NULL;NULL;");
			}

			if (educationCoord != null) {
				personWriter.write(String.valueOf(educationCoord.getX()) + ";");
				personWriter.write(String.valueOf(educationCoord.getX()));
			} else {
				personWriter.write("NULL;NULL");
			}

			personWriter.write("\n");
			personWriter.flush();
		}

		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			Id<Person> personId = household.getMemberIds().get(0);
			Person person = scenario.getPopulation().getPersons().get(personId);

			Coord homeCoord = null;

			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = (Activity) element;

					if (activity.getType().equals("home")) {
						homeCoord = activity.getCoord();
						break;
					}
				}
			}

			householdWriter.write(String.valueOf(household.getIncome().getIncome()) + ";");
			householdWriter.write(String.valueOf(homeCoord.getX()) + ";");
			householdWriter.write(String.valueOf(homeCoord.getY()) + "\n");
		}

		activityWriter.close();
		personWriter.close();
		householdWriter.close();
	}
}
