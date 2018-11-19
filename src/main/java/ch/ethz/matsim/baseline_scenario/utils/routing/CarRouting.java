package ch.ethz.matsim.baseline_scenario.utils.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;

public class CarRouting {
	final private int numberOfThreads;
	final private Network network;

	public CarRouting(int numberOfThreads, Network network) {
		this.network = network;
		this.numberOfThreads = numberOfThreads;
	}

	public void run(Population population, TravelTime travelTime) throws InterruptedException {
		run(population.getPersons().values(), travelTime);
	}

	public void run(Collection<? extends Person> persons, TravelTime travelTime) throws InterruptedException {
		Iterator<? extends Person> personIterator = persons.iterator();

		List<Thread> threads = new ArrayList<>(numberOfThreads);

		final Counter counter = new Counter("", " legs routed");
		long chunkSize = 10000;

		for (int i = 0; i < numberOfThreads; i++) {
			LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(network,
					new OnlyTimeDependentTravelDisutility(travelTime), travelTime);

			threads.add(new Thread(() -> {
				while (true) {
					List<Person> tasks = new LinkedList<>();

					synchronized (personIterator) {
						while (personIterator.hasNext() && tasks.size() < chunkSize) {
							tasks.add(personIterator.next());
						}
					}

					if (tasks.size() == 0) {
						return;
					}

					for (Person person : tasks) {
						for (Plan plan : person.getPlans()) {
							for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan,
									new StageActivityTypesImpl())) {
								if (trip.getLegsOnly().get(0).getMode().equals(TransportMode.car)) {
									Id<Link> originId = trip.getOriginActivity().getLinkId();
									Id<Link> destinationId = trip.getDestinationActivity().getLinkId();
									double departureTime = trip.getOriginActivity().getEndTime();

									Path path = router.calcLeastCostPath(network.getLinks().get(originId).getToNode(),
											network.getLinks().get(destinationId).getFromNode(), departureTime, null,
											null);

									LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
									NetworkRoute route = (NetworkRoute) factory.createRoute(originId, destinationId);
									route.setLinkIds(originId, NetworkUtils.getLinkIds(path.links), destinationId);
									route.setTravelTime((int) path.travelTime);
									route.setTravelCost(path.travelCost);
									route.setDistance(RouteUtils.calcDistance(route, 1.0, 1.0, network));

									trip.getLegsOnly().get(0).setRoute(route);
								}
							}
						}

						synchronized (counter) {
							counter.incCounter();
						}
					}
				}
			}));
		}

		for (Thread thread : threads) {
			thread.run();
		}

		for (Thread thread : threads) {
			thread.join();
		}
	}
}
