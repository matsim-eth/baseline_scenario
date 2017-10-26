package ch.ethz.matsim.baseline_scenario.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class CountTravelTime implements TravelTime {
	final private Map<Link, List<Double>> travelTimes = new HashMap<>();
	
	public CountTravelTime(double scaling, Network network, Collection<Person> persons, TravelTime previousTravelTime) {
		Map<Link, List<Integer>> counts = new HashMap<>();
		int bins = 30 * 3600 / 3600;
		
		for (Link link : network.getLinks().values()) {
			counts.put(link, new ArrayList<>(Collections.nCopies(bins, 0)));
			travelTimes.put(link, new ArrayList<>(Collections.nCopies(bins, 0.0)));
		}
		
		for (Person person : persons) {
			Plan plan = person.getSelectedPlan();
			
			for (Leg leg : TripStructureUtils.getLegs(plan)) {
				if (leg.getMode().equals("car")) {
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					
					double time = leg.getDepartureTime();
					
					for (Id<Link> linkId : route.getLinkIds()) {
						Link link = network.getLinks().get(linkId);
						int timeBin = getTimeBin(time);
						
						counts.get(link).set(timeBin, counts.get(link).get(timeBin) + 1);
						time += previousTravelTime.getLinkTravelTime(link, time, null, null);
					}
				}
			}
		}
		
		for (Map.Entry<Link, List<Integer>> item : counts.entrySet()) {
			for (int i = 0; i < item.getValue().size(); i++) {
				Link link = item.getKey();
				double count = item.getValue().get(i);
				
				double freeflowTravelTime = link.getLength() / link.getFreespeed();
				double capacity = link.getCapacity();
				
				double travelTime = freeflowTravelTime * (1.0 + 0.15 * Math.pow(count / (capacity * scaling), 4.0));

				travelTimes.get(link).set(i, travelTime);
			}
		}
	}
	
	private int getTimeBin(double time) {
		return Math.max(0, Math.min((int) (time / 3600.0), 30 * 3600 / 3600));
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return travelTimes.get(link).get(getTimeBin(time));
	}
}
