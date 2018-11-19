package ch.ethz.matsim.baseline_scenario;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

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
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.vividsolutions.jts.geom.Coordinate;

public class IDFAnalysis {
	static public void main(String[] args) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(args[0]);
		new HouseholdsReaderV10(scenario.getHouseholds()).readFile(args[1]);

		PolylineFeatureFactory odFactory = new PolylineFeatureFactory.Builder().setCrs(CRS.decode("EPSG:2154"))
				.setName("od").addAttribute("type", String.class).create();
		PointFeatureFactory homeFactory = new PointFeatureFactory.Builder().setCrs(CRS.decode("EPSG:2154"))
				.setName("home").create();
		PointFeatureFactory workFactory = new PointFeatureFactory.Builder().setCrs(CRS.decode("EPSG:2154"))
				.setName("work").create();
		PointFeatureFactory educationFactory = new PointFeatureFactory.Builder().setCrs(CRS.decode("EPSG:2154"))
				.setName("education").create();
		PointFeatureFactory householdFactory = new PointFeatureFactory.Builder().setCrs(CRS.decode("EPSG:2154"))
				.setName("household").addAttribute("income", Double.class).create();
		PointFeatureFactory activityFactory = new PointFeatureFactory.Builder().setCrs(CRS.decode("EPSG:2154"))
				.setName("activity").addAttribute("purpose", String.class).create();

		Collection<SimpleFeature> odCollection = new LinkedList<>();
		Collection<SimpleFeature> homeCollection = new LinkedList<>();
		Collection<SimpleFeature> workCollection = new LinkedList<>();
		Collection<SimpleFeature> educationCollection = new LinkedList<>();
		Collection<SimpleFeature> householdCollection = new LinkedList<>();
		Collection<SimpleFeature> activityCollection = new LinkedList<>();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Coord homeCoord = null;
			Coord workCoord = null;
			Coord educationCoord = null;

			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = (Activity) element;

					if (!activity.getType().contains("interaction")) {
						activityCollection.add(activityFactory.createPoint(activity.getCoord(),
								new Object[] { activity.getType() }, null));
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

			homeCollection.add(homeFactory.createPoint(homeCoord, new Object[] {}, null));

			if (workCoord != null) {
				workCollection.add(workFactory.createPoint(workCoord, new Object[] {}, null));
			}

			if (educationCoord != null) {
				educationCollection.add(educationFactory.createPoint(educationCoord, new Object[] {}, null));
			}

			if (workCoord != null || educationCoord != null) {
				String type = workCoord != null ? "work" : "education";
				Coord coord = workCoord != null ? workCoord : educationCoord;

				SimpleFeature odPair = odFactory
						.createPolyline(new Coordinate[] { new Coordinate(homeCoord.getX(), homeCoord.getY()),
								new Coordinate(coord.getX(), coord.getY()) }, new Object[] { type }, null);
				odCollection.add(odPair);
			}
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

			householdCollection.add(
					householdFactory.createPoint(homeCoord, new Object[] { household.getIncome().getIncome() }, null));
		}

		ShapeFileWriter.writeGeometries(homeCollection, "home_by_person.shp");
		ShapeFileWriter.writeGeometries(workCollection, "work_by_person.shp");
		ShapeFileWriter.writeGeometries(educationCollection, "education_by_person.shp");
		ShapeFileWriter.writeGeometries(activityCollection, "activities.shp");
		ShapeFileWriter.writeGeometries(householdCollection, "households_with_income.shp");
		ShapeFileWriter.writeGeometries(odCollection, "commute_od_pairs.shp");
	}
}
