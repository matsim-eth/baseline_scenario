package ch.ethz.matsim.baseline_scenario.preparation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.Provider;

import ch.ethz.matsim.baseline_scenario.transit.connection.DefaultTransitConnectionFinder;
import ch.ethz.matsim.baseline_scenario.transit.connection.TransitConnectionFinder;
import ch.ethz.matsim.baseline_scenario.transit.routing.BaselineTransitRoutingModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouter;
import ch.ethz.matsim.baseline_scenario.transit.routing.EnrichedTransitRouter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DefaultDepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.LeastCostRaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.RaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorFactory;

public class Routing {
	private final Config config;

	private final Network fullNetwork;
	private final Network roadNetwork;

	private final TransitSchedule transitSchedule;

	private int numberOfThreads;

	public Routing(Config config, Network network, Network roadNetwork, TransitSchedule transitSchedule) {
		this.config = config;
		this.fullNetwork = network;
		this.roadNetwork = roadNetwork;
		this.transitSchedule = transitSchedule;

		this.numberOfThreads = Runtime.getRuntime().availableProcessors();
	}

	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	static public void main(String[] args) throws SecurityException, NoSuchMethodException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, InterruptedException {
		String configPath = args[0];
		String networkPath = args[1];
		String inputPopulationPath = args[2];
		String inputSchedulePath = args[3];
		String outputPopulationPath = args[4];

		Config config = ConfigUtils.loadConfig(configPath);
		Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);
		new PopulationReader(scenario).readFile(inputPopulationPath);
		new TransitScheduleReader(scenario).readFile(inputSchedulePath);
		
		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));

		Routing routing = new Routing(config, scenario.getNetwork(), roadNetwork, scenario.getTransitSchedule());
		routing.setNumberOfThreads(config.global().getNumberOfThreads());
		routing.run(scenario.getPopulation());

		new PopulationWriter(scenario.getPopulation()).write(outputPopulationPath);
	}

	public void run(Population population) throws InterruptedException {
		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();
		AtomicLong numberOfProcessedPersons = new AtomicLong(0);
		long numberOfPersons = population.getPersons().size();

		List<Thread> threads = new LinkedList<>();

		RaptorParametersForPerson parametersForPerson = new DefaultRaptorParametersForPerson(config);
		RaptorRouteSelector routeSelector = new LeastCostRaptorRouteSelector();
		RaptorIntermodalAccessEgress accessEgress = new DefaultRaptorIntermodalAccessEgress();

		SwissRailRaptorFactory factory = new SwissRailRaptorFactory(transitSchedule, config, fullNetwork,
				parametersForPerson, routeSelector, accessEgress, config.plans(), population, Collections.emptyMap());

		Thread statusThread = new Thread(() -> {
			long previousNumberOfProcessedPersons = 0;

			try {
				long currentNumberOfProcessedPersons = 0;

				do {
					Thread.sleep(1000);
					currentNumberOfProcessedPersons = numberOfProcessedPersons.get();

					if (currentNumberOfProcessedPersons > previousNumberOfProcessedPersons) {
						System.out.println(String.format("Routing... %d / %d (%.2f%%)", currentNumberOfProcessedPersons,
								numberOfPersons, 100.0 * currentNumberOfProcessedPersons / numberOfPersons));
						previousNumberOfProcessedPersons = currentNumberOfProcessedPersons;
					}
				} while (currentNumberOfProcessedPersons < numberOfPersons);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
		statusThread.start();

		for (int i = 0; i < numberOfThreads; i++) {
			threads.add(new Thread(new RoutingRunner(config, population, transitSchedule, fullNetwork, roadNetwork,
					personIterator, numberOfProcessedPersons, factory)));
		}

		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		statusThread.join();
	}

	static public class RoutingRunner implements Runnable {
		private final Config config;
		private final Network carNetwork;
		private final Network fullNetwork;
		private final TransitSchedule transitSchedule;
		private final Population population;
		private final Iterator<? extends Person> personIterator;
		private final AtomicLong numberOfProcessedPersons;
		private final SwissRailRaptorFactory factory;

		public RoutingRunner(Config config, Population population, TransitSchedule transitSchedule, Network fullNetwork,
				Network carNetwork, Iterator<? extends Person> personIterator, AtomicLong numberOfProcessedPersons,
				SwissRailRaptorFactory factory) {
			this.config = config;
			this.carNetwork = carNetwork;
			this.personIterator = personIterator;
			this.numberOfProcessedPersons = numberOfProcessedPersons;
			this.factory = factory;
			this.population = population;
			this.fullNetwork = fullNetwork;
			this.transitSchedule = transitSchedule;
		}

		@Override
		public void run() {
			try {
				PlanRouter planRouter = new PlanRouter(
						createRouter(config, carNetwork, fullNetwork, transitSchedule, population, factory));
				XY2Links xy = new XY2Links(carNetwork, null);

				while (true) {
					Person person = null;

					synchronized (personIterator) {
						if (personIterator.hasNext()) {
							person = personIterator.next();
						} else {
							return;
						}
					}

					xy.run(person);
					planRouter.run(person);

					numberOfProcessedPersons.incrementAndGet();
				}

			} catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}

	static public TripRouter createRouter(Config config, Network carNetwork, Network fullNetwork,
			TransitSchedule schedule, Population population, SwissRailRaptorFactory factory) throws SecurityException,
			NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();
		PopulationFactory populationFactory = PopulationUtils.getFactory();

		TripRouter.Builder tripRouterBuilder = new TripRouter.Builder(config);
		tripRouterBuilder.setMainModeIdentifier(mainModeIdentifier);

		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);

		LeastCostPathCalculator carRouter = new DijkstraFactory().createPathCalculator(carNetwork, travelDisutility,
				travelTime);
		NetworkRoutingModule carRoutingModule = new NetworkRoutingModule("car", populationFactory, carNetwork,
				carRouter);
		tripRouterBuilder.putRoutingModule("car", new RoutingModuleProvider(carRoutingModule));
		tripRouterBuilder.putRoutingModule("car_passenger", new RoutingModuleProvider(carRoutingModule));	
		
		ModeRoutingParams walkParams = config.plansCalcRoute().getModeRoutingParams().get("walk");
		TeleportationRoutingModule walkRoutingModule = new TeleportationRoutingModule("walk", populationFactory,
				walkParams.getTeleportedModeSpeed(), walkParams.getBeelineDistanceFactor());
		tripRouterBuilder.putRoutingModule("walk", new RoutingModuleProvider(walkRoutingModule));

		ModeRoutingParams bikeParams = config.plansCalcRoute().getModeRoutingParams().get("bike");
		TeleportationRoutingModule bikeRoutingModule = new TeleportationRoutingModule("bike", populationFactory,
				bikeParams.getTeleportedModeSpeed(), bikeParams.getBeelineDistanceFactor());
		tripRouterBuilder.putRoutingModule("bike", new RoutingModuleProvider(bikeRoutingModule));

		SwissRailRaptor raptor = factory.get();
		DepartureFinder departureFinder = new DefaultDepartureFinder();
		TransitConnectionFinder connectionFinder = new DefaultTransitConnectionFinder(departureFinder);
		EnrichedTransitRouter transitRouter = new DefaultEnrichedTransitRouter(raptor, schedule, connectionFinder,
				fullNetwork, walkParams.getBeelineDistanceFactor(), config.transitRouter().getAdditionalTransferTime());
		RoutingModule ptRoutingModule = new BaselineTransitRoutingModule(transitRouter, schedule);
		tripRouterBuilder.putRoutingModule("pt", new RoutingModuleProvider(ptRoutingModule));

		if (config.plansCalcRoute().getModeRoutingParams().containsKey("outside")) {
			ModeRoutingParams outsideParams = config.plansCalcRoute().getModeRoutingParams().get("outside");
			TeleportationRoutingModule outsideRoutingModule = new TeleportationRoutingModule("outside",
					populationFactory, outsideParams.getTeleportedModeSpeed(),
					outsideParams.getBeelineDistanceFactor());
			tripRouterBuilder.putRoutingModule("outside", new RoutingModuleProvider(outsideRoutingModule));
		}

		Method method = TripRouter.Builder.class.getDeclaredMethod("builder");
		method.setAccessible(true);
		return (TripRouter) method.invoke(tripRouterBuilder);
	}

	static public class RoutingModuleProvider implements Provider<RoutingModule> {
		private final RoutingModule routingModule;

		public RoutingModuleProvider(RoutingModule routingModule) {
			this.routingModule = routingModule;
		}

		@Override
		public RoutingModule get() {
			return routingModule;
		}
	}
}
