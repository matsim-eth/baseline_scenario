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
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;

import ch.ethz.matsim.mode_choice.utils.QueueBasedThreadSafeDijkstra;

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
		QueueBasedThreadSafeDijkstra router = new QueueBasedThreadSafeDijkstra(numberOfThreads, network,
				new OnlyTimeDependentTravelDisutility(travelTime), travelTime);

		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
		final Counter counter = new Counter("", " legs routed");

		for (Person person : persons) {
			for (Plan plan : person.getPlans()) {
				for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan, new StageActivityTypesImpl())) {
					if (trip.getLegsOnly().get(0).getMode().equals(TransportMode.car)) {
						executor.execute(new Runnable() {
							@Override
							public void run() {
								route(router, trip.getLegsOnly().get(0), trip.getOriginActivity().getLinkId(),
										trip.getDestinationActivity().getLinkId(),
										trip.getOriginActivity().getEndTime());

								synchronized (counter) {
									counter.incCounter();
								}
							}
						});
					}
				}
			}
		}

		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

		router.close();
	}

	private void route(LeastCostPathCalculator router, Leg leg, Id<Link> originId, Id<Link> destinationId,
			double departureTime) {
		Path path = router.calcLeastCostPath(network.getLinks().get(originId).getToNode(),
				network.getLinks().get(destinationId).getFromNode(), departureTime, null, null);

		LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
		NetworkRoute route = (NetworkRoute) factory.createRoute(originId, destinationId);
		route.setLinkIds(originId, NetworkUtils.getLinkIds(path.links), destinationId);
		route.setTravelTime((int) path.travelTime);
		route.setTravelCost(path.travelCost);
		route.setDistance(RouteUtils.calcDistance(route, 1.0, 1.0, network));

		leg.setRoute(route);
	}
}
