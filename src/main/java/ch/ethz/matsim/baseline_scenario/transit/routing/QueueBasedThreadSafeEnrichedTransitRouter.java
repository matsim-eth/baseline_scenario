package ch.ethz.matsim.baseline_scenario.transit.routing;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;

import javax.inject.Provider;
import java.util.List;
import java.util.concurrent.*;

public class QueueBasedThreadSafeEnrichedTransitRouter implements ThreadSafeEnrichedTransitRouter {
	final private ExecutorService executor;
	final private BlockingQueue<EnrichedTransitRouter> instanceQueue = new LinkedBlockingQueue<>();

	public QueueBasedThreadSafeEnrichedTransitRouter(int numberOfInstances, Provider<EnrichedTransitRouter> factory) {
		executor = Executors.newFixedThreadPool(numberOfInstances);

		try {
			for (int i = 0; i < numberOfInstances; i++) {
				instanceQueue.put(factory.get());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<Leg> calculateRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
                                    Person person) {
		try {
			return executor.submit(new Callable<List<Leg>>() {
				@Override
				public List<Leg> call() throws Exception {
					EnrichedTransitRouter instance = instanceQueue.take();
					List<Leg> result = instance.calculateRoute(fromFacility, toFacility, departureTime, person);
					instanceQueue.put(instance);
					return result;
				}
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

}
