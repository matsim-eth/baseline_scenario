package ch.ethz.matsim.baseline_scenario.preparation;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class Downsample {
	final private double probability;
	final private Random random;

	public Downsample(double fraction, Random random) {
		this.probability = fraction;
		this.random = random;
	}

	public void run(Population population) throws InterruptedException {
		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();

		long numberOfPersons = population.getPersons().size();
		AtomicLong numberOfProcessedPersons = new AtomicLong(0);

		Thread thread = new Thread(() -> {
			try {
				long currentNumberOfProcessedPersons = 0;

				do {
					Thread.sleep(1000);
					currentNumberOfProcessedPersons = numberOfProcessedPersons.get();

					System.out.println(String.format("Downsampling ... %d/%d (%.2f%%)", currentNumberOfProcessedPersons,
							numberOfPersons, 100.0 * numberOfProcessedPersons.get() / numberOfPersons));
				} while (currentNumberOfProcessedPersons < numberOfPersons);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});

		thread.start();

		while (personIterator.hasNext()) {
			personIterator.next();

			if (random.nextDouble() >= probability) {
				personIterator.remove();
			}

			numberOfProcessedPersons.incrementAndGet();
		}

		thread.join();
	}

	static public void main(String[] args) throws InterruptedException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(args[0]);

		double probability = Double.parseDouble(args[1]);
		new Downsample(probability, new Random()).run(scenario.getPopulation());

		new PopulationWriter(scenario.getPopulation()).write(args[2]);
	}
}