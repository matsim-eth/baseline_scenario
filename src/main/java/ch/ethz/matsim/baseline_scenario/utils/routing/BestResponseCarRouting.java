package ch.ethz.matsim.baseline_scenario.utils.routing;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

public class BestResponseCarRouting {
	final private CarRouting carRouting;
	
	public BestResponseCarRouting(int numberOfThreads, Network network) {
		this.carRouting = new CarRouting(numberOfThreads, network);
	}
	
	public void run(Population population) throws InterruptedException {
		carRouting.run(population, new FreeSpeedTravelTime());
	}

	public void run(Collection<? extends Person> persons) throws InterruptedException {
		carRouting.run(persons, new FreeSpeedTravelTime());
	}
}
