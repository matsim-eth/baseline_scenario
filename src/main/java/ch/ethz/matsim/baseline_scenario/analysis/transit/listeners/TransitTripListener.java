package ch.ethz.matsim.baseline_scenario.analysis.transit.listeners;

import ch.ethz.matsim.baseline_scenario.analysis.transit.TransitTripItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class TransitTripListener implements ActivityStartEventHandler, ActivityEndEventHandler,
		PersonDepartureEventHandler, TeleportationArrivalEventHandler, LinkLeaveEventHandler,
		TransitDriverStartsEventHandler, VehicleLeavesTrafficEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	final private StageActivityTypes stageActivityTypes;
	final private Network network;
	private final TransitSchedule schedule;
	private final HashMap<Id<TransitLine>, Set<Id<TransitRoute>>> removedLines;

	final private List<TransitTripItem> trips = new LinkedList<>();
	final private Map<Id<Person>, Integer> tripIndex = new HashMap<>();
	final private Map<Id<Person>, TransitTripListenerItem> ongoing = new HashMap<>();
	final private Set<Id<Person>> ptDrivers = new HashSet<>();
	final private Map<Id<Vehicle>, PTVehicle> ptVehicles = new HashMap<>();

	public TransitTripListener(StageActivityTypes stageActivityTypes, Network network, TransitSchedule schedule,
							   HashMap<Id<TransitLine>, Set<Id<TransitRoute>>> removedLines) {
		this.stageActivityTypes = stageActivityTypes;
		this.network = network;
		this.schedule = schedule;
		this.removedLines = removedLines;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (!stageActivityTypes.isStageActivity(event.getActType())) {
			Integer tripId = tripIndex.get(event.getPersonId());

			if (tripId == null) {
				tripId = 0;
			} else {
				tripId += 1;
			}

			TransitTripListenerItem item = new TransitTripListenerItem();
			item.personId = event.getPersonId();
			item.personTripId = tripId;
			item.origin = network.getLinks().get(event.getLinkId()).getCoord();
			item.startTime = event.getTime();

			ongoing.put(event.getPersonId(), item);
			tripIndex.put(event.getPersonId(), tripId);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		TransitTripListenerItem item = ongoing.get(event.getPersonId());

		if (item != null) {
			item.mode = event.getLegMode();
			item.legDepartureTime = event.getTime();

			if (item.mode.equals("pt")) {
				item.numberOfTransfers += 1;
			}
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event)	{
		ptDrivers.add(event.getDriverId());
		ptVehicles.put(event.getVehicleId(),
				new PTVehicle(event.getTransitLineId(), event.getTransitRouteId(), event.getVehicleId()));
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event)	{
		if(ptVehicles.containsKey(event.getVehicleId()))	{
			ptVehicles.remove(event.getVehicleId());
		}
		if(ptDrivers.contains(event.getPersonId()))	{
			ptDrivers.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event)	{
		if(ptVehicles.containsKey(event.getVehicleId()))	{
			PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
			vehicle.setCurrentStop(schedule.getFacilities().get(event.getFacilityId()));
		}
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event)	{
		PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
		for (Id<Person> pax : vehicle.getPassengersId()) {
			if(vehicle.isFirstStop.get(pax))	{
				TransitTripListenerItem item = ongoing.get(pax);
				if(item != null)	{
					item.vehDepartureTime = event.getTime();
					double waitingTime = event.getTime() - item.legDepartureTime;
					if (item.waitingForFirstTransitEvent) {
						item.waitingForFirstTransitEvent = false;
						item.firstWaitingTime = waitingTime;
					}
					else	{
						item.waitingTime += waitingTime;
					}
				}
				vehicle.handledFirstStop(pax);
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		TransitTripListenerItem item = ongoing.get(event.getPersonId());

		if(item != null && ptVehicles.containsKey(event.getVehicleId()) && !ptDrivers.contains(event.getPersonId())) {
			PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
			vehicle.addPassenger(event.getPersonId());

			item.accessStopCoords = vehicle.getCurrentStop().getCoord();

			if(event.getVehicleId().toString().contains("para"))	{
				item.partMinibusTrip = true;
			}
			else	{
				item.fullMinibusTrip = false;
			}
			if(removedLines.containsKey(vehicle.transitLineId))	{
				if(removedLines.get(vehicle.transitLineId).contains(vehicle.transitRouteId))	{
					item.partRefTrip = true;
				}
				else	{
					item.fullRefTrip = false;
				}
			}
			else	{
				item.fullRefTrip = false;
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		TransitTripListenerItem item = ongoing.get(event.getPersonId());

		if(item != null && ptVehicles.containsKey(event.getVehicleId()) && !ptDrivers.contains(event.getPersonId())) {
			PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
			double distance = vehicle.removePassenger(event.getPersonId());
			item.inVehicleDistance = distance;

			double euclDistance = CoordUtils.calcEuclideanDistance(item.accessStopCoords, vehicle.currentStop.getCoord());
			item.inVehicleCrowflyDistance = euclDistance;
			item.inVehicleTime = event.getTime() - item.vehDepartureTime;
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event)	{
		if(ptVehicles.containsKey(event.getVehicleId())) {
			ptVehicles.get(event.getVehicleId()).incDistance(network.getLinks().get(event.getLinkId()).getLength());
		}
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		TransitTripListenerItem item = ongoing.get(event.getPersonId());

		if (item != null) {
			if (item.mode.equalsIgnoreCase(TransportMode.access_walk)) {
				item.accessDistance = event.getDistance();
			}
			else if (item.mode.equalsIgnoreCase(TransportMode.egress_walk)) {
				item.egressDistance = event.getDistance();
			}
			else if (item.mode.equalsIgnoreCase(TransportMode.transit_walk)) {
				item.transferDistance += event.getDistance();
				item.transferTime += event.getTime() - item.legDepartureTime;
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!stageActivityTypes.isStageActivity(event.getActType())) {
			TransitTripListenerItem item = ongoing.remove(event.getPersonId());

			if (item != null && item.inVehicleTime != 0) {
				if (item.mode.equals("pt") || (item.mode.contains("walk") && !item.mode.equals("walk"))) {
					item.totTripTime = event.getTime() - item.startTime;
					item.destination = network.getLinks().get(event.getLinkId()).getCoord();
					item.numberOfTransfers = Math.max(item.numberOfTransfers, 0);
					trips.add(item);
				}
			}
		}
	}

	public Collection<TransitTripItem> getTransitTripItems() {
		return trips;
	}

	private class PTVehicle {
		private final Id transitLineId;
		private final Id transitRouteId;
		private final Id vehicleId;

		private final Map<Id<Person>, Double> passengers = new HashMap<>();
		private final Map<Id<Person>, Boolean> isFirstStop = new HashMap<>();
		private double distance;
		private double departureTime = 0.0;
		private TransitStopFacility currentStop;

		public PTVehicle(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId, Id<Vehicle> vehicleId) {
			this.transitLineId = transitLineId;
			this.transitRouteId = transitRouteId;
			this.vehicleId = vehicleId;
		}

		public void incDistance(double linkDistance) {
			distance += linkDistance;
		}

		public Set<Id<Person>> getPassengersId() {
			return passengers.keySet();
		}

		public void addPassenger(Id<Person> passengerId) {
			passengers.put(passengerId, distance);
			isFirstStop.put(passengerId, true);
		}

		public void handledFirstStop(Id<Person> passengerId)	{
			isFirstStop.put(passengerId, false);
		}

		public double removePassenger(Id passengerId) {
			return distance - passengers.remove(passengerId);
		}

		public double getDepartureTime() {
			return departureTime;
		}

		public void setDepartureTime(double departureTime) {
			this.departureTime = departureTime;
		}
		
		public void setCurrentStop(TransitStopFacility stop)	{
			this.currentStop = stop;
		}
		
		public TransitStopFacility getCurrentStop()	{
			return this.currentStop;
		}
	}
}
