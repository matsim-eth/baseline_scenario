package ch.ethz.matsim.baseline_scenario.paris;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import ch.ethz.matsim.baseline_scenario.config.CommandLine;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;

public class ImputeInnerParisAttribute {
	private static Map<String, IRIS> readIRIS(File path) throws MalformedURLException, IOException {
		DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", path.toURI().toURL()));
		SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
		SimpleFeatureCollection featureCollection = featureSource.getFeatures();
		SimpleFeatureIterator featureIterator = featureCollection.features();

		Map<String, IRIS> iris = new HashMap<>();

		while (featureIterator.hasNext()) {
			SimpleFeature feature = featureIterator.next();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			String code = (String) feature.getAttribute("CODE_IRIS");
			iris.put(code, new IRIS(geometry, code));
		}

		featureIterator.close();
		dataStore.dispose();

		return iris;
	}

	private static Optional<IRIS> findIRIS(Coord coord, Collection<IRIS> iris) {
		for (IRIS candidate : iris) {
			if (candidate.containsCoordinate(coord)) {
				return Optional.of(candidate);
			}
		}

		return Optional.empty();
	}

	public static void main(String[] args) throws MalformedURLException, IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("iris-path", "input-path", "output-path") //
				.build();

		Map<String, IRIS> iris = readIRIS(new File(cmd.getOptionStrict("iris-path")));
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		PopulationReader readerPop = new PopulationReader(scenario);
		readerPop.readFile(cmd.getOptionStrict("input-path"));

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();

			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;

					if (!activity.getType().equals("pt interaction")) {
						Optional<IRIS> linkIris = findIRIS(activity.getCoord(), iris.values());

						if (linkIris.isPresent()) {
							activity.getAttributes().putAttribute("innerParis", true);
						} else {
							activity.getAttributes().putAttribute("innerParis", false);
						}
					}
				}
			}
		}

		PopulationWriter popWriter = new PopulationWriter(scenario.getPopulation());
		popWriter.write(cmd.getOptionStrict("output-path"));
	}
}