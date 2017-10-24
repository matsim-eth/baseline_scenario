package ch.ethz.matsim.baseline_scenario.utils.routing;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.Counter;

import ch.ethz.matsim.mode_choice.utils.QueueBasedThreadSafeDijkstra;

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
