package ch.ethz.matsim.baseline_scenario.utils;

import java.util.Iterator;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class Downsample {
	final private double probability;
	final private Random random;

	public Downsample(double fraction, Random random) {
		this.probability = fraction;
		this.random = random;
	}

	public void run(Population population) {
		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();

		while (personIterator.hasNext()) {
			personIterator.next();

			if (random.nextDouble() >= probability) {
				personIterator.remove();
			}
		}
	}

	static public void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(args[0]);

		double probability = Double.parseDouble(args[1]);
		new Downsample(probability, new Random()).run(scenario.getPopulation());

		new PopulationWriter(scenario.getPopulation()).write(args[2]);
	}
}