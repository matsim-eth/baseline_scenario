package ch.ethz.matsim.baseline_scenario.analysis.readers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.baseline_scenario.analysis.TripItem;
import ch.ethz.matsim.baseline_scenario.analysis.utils.HomeActivityTypes;

public class PopulationTripReader {
	final private Network network;
	final private StageActivityTypes stageActivityTypes;
	final private HomeActivityTypes homeActivityTypes;
	final private MainModeIdentifier mainModeIdentifier;

	public PopulationTripReader(Network network, StageActivityTypes stageActivityTypes,
			HomeActivityTypes homeActivityTypes, MainModeIdentifier mainModeIdentifier) {
		this.network = network;
		this.stageActivityTypes = stageActivityTypes;
		this.homeActivityTypes = homeActivityTypes;
		this.mainModeIdentifier = mainModeIdentifier;
	}

	public Collection<TripItem> readTrips(String populationPath) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(populationPath);
		return readTrips(scenario.getPopulation());
	}

	public Collection<TripItem> readTrips(Population population) {
		List<TripItem> tripItems = new LinkedList<>();

		for (Person person : population.getPersons().values()) {
			List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan(),
					stageActivityTypes);

			for (TripStructureUtils.Trip trip : trips) {
				boolean isHomeTrip = homeActivityTypes.isHomeActivity(trip.getDestinationActivity().getType());

				tripItems.add(new TripItem(trip.getOriginActivity().getCoord(),
						trip.getDestinationActivity().getCoord(), trip.getOriginActivity().getEndTime(),
						trip.getDestinationActivity().getStartTime() - trip.getOriginActivity().getEndTime(),
						getNetworkDistance(trip), mainModeIdentifier.identifyMainMode(trip.getTripElements()),
						isHomeTrip ? trip.getOriginActivity().getType() : trip.getDestinationActivity().getType(),
						isHomeTrip));
			}
		}

		return tripItems;
	}

	private double getNetworkDistance(TripStructureUtils.Trip trip) {
		double distance = Double.NaN;

		if (mainModeIdentifier.identifyMainMode(trip.getTripElements()).equals("car")) {
			NetworkRoute route = (NetworkRoute) trip.getLegsOnly().get(0).getRoute();

			if (route != null) {
				distance = 0.0;

				for (Id<Link> linkId : route.getLinkIds()) {
					distance += network.getLinks().get(linkId).getLength();
				}
			}
		}

		return distance;
	}
}
