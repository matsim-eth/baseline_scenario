package ch.ethz.matsim.baseline_scenario.transit;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Singleton;

@Singleton
public class BaselineTransitEngine implements DepartureHandler, MobsimEngine {
	final private TransitSchedule transitSchedule;
	final private PreparedTransitSchedule preparedTransitSchedule;
	private InternalInterface internalInterface;
	final private EventsManager eventsManager;
	final private Network network;

	final private PriorityQueue<AgentDeparture> departures = new PriorityQueue<>();
	final private PriorityQueue<AgentArrival> arrivals = new PriorityQueue<>();

	private class AgentDeparture implements Comparable<AgentDeparture> {
		final public MobsimAgent agent;
		final public double departureTime;
		final public Id<Link> departureLinkId;

		public AgentDeparture(MobsimAgent agent, double departureTime, Id<Link> departureLinkId) {
			this.agent = agent;
			this.departureTime = departureTime;
			this.departureLinkId = departureLinkId;
		}

		@Override
		public int compareTo(AgentDeparture other) {
			return Double.compare(departureTime, other.departureTime);
		}
	}

	private class AgentArrival implements Comparable<AgentArrival> {
		final public MobsimAgent agent;
		final public double arrivalTime;
		final public Id<Link> arrivalLinkId;
		final public PublicTransitEvent event;

		public AgentArrival(MobsimAgent agent, double arrivalTime, Id<Link> arrivalLinkId, PublicTransitEvent event) {
			this.agent = agent;
			this.arrivalTime = arrivalTime;
			this.arrivalLinkId = arrivalLinkId;
			this.event = event;
		}

		@Override
		public int compareTo(AgentArrival other) {
			return Double.compare(arrivalTime, other.arrivalTime);
		}
	}

	public BaselineTransitEngine(EventsManager eventsManager, TransitSchedule transitSchedule, Network network) {
		this.eventsManager = eventsManager;
		this.transitSchedule = transitSchedule;
		this.network = network;
		this.preparedTransitSchedule = new PreparedTransitSchedule(transitSchedule);
	}

	private TransitRouteStop getFirstStop(TransitRoute transitRoute, TransitStopFacility facility) {
		for (TransitRouteStop stop : transitRoute.getStops()) {
			if (stop.getStopFacility() == facility) {
				return stop;
			}
		}

		return null;
	}

	private TransitRouteStop getLastStop(TransitRoute transitRoute, TransitStopFacility facility) {
		TransitRouteStop result = null;

		for (TransitRouteStop stop : transitRoute.getStops()) {
			if (stop.getStopFacility() == facility) {
				result = stop;
			}
		}

		return result;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> departureLinkId) {
		if (agent.getMode().equals("pt")) {
			Leg leg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();

			TransitLine transitLine = transitSchedule.getTransitLines().get(route.getLineId());
			TransitRoute transitRoute = transitLine.getRoutes().get(route.getRouteId());

			TransitRouteStop accessStop = getFirstStop(transitRoute,
					transitSchedule.getFacilities().get(route.getAccessStopId()));
			TransitRouteStop egressStop = getLastStop(transitRoute,
					transitSchedule.getFacilities().get(route.getEgressStopId()));

			double vehicleDepartureTime = preparedTransitSchedule.getNextDepartureTime(transitRoute, accessStop, now);

			while (vehicleDepartureTime < now) {
				vehicleDepartureTime += 24.0 * 3600.0;
			}

			double inVehicleTime = egressStop.getArrivalOffset() - accessStop.getDepartureOffset();
			double arrivalTime = vehicleDepartureTime + inVehicleTime;

			if (arrivalTime < vehicleDepartureTime || arrivalTime < now) {
				throw new IllegalStateException();
			}

			if (arrivalTime == vehicleDepartureTime) {
				arrivalTime += 1.0;
			}

			double travelDistance = RouteUtils.calcDistance(route, transitSchedule, network);
			Id<Link> arrivalLinkId = egressStop.getStopFacility().getLinkId();

			if (!accessStop.getStopFacility().getLinkId().equals(departureLinkId)) {
				throw new IllegalStateException();
			}

			PublicTransitEvent transitEvent = new PublicTransitEvent(arrivalTime, agent.getId(), transitLine.getId(),
					transitRoute.getId(), accessStop.getStopFacility().getId(), egressStop.getStopFacility().getId(),
					vehicleDepartureTime, travelDistance);

			internalInterface.registerAdditionalAgentOnLink(agent);
			departures.add(new AgentDeparture(agent, vehicleDepartureTime, departureLinkId));
			arrivals.add(new AgentArrival(agent, arrivalTime, arrivalLinkId, transitEvent));

			return true;
		}

		return false;
	}

	@Override
	public void doSimStep(double time) {
		while (!departures.isEmpty() && departures.peek().departureTime <= time) {
			AgentDeparture departure = departures.poll();
			internalInterface.unregisterAdditionalAgentOnLink(departure.agent.getId(), departure.departureLinkId);
		}

		while (!arrivals.isEmpty() && arrivals.peek().arrivalTime <= time) {
			AgentArrival arrival = arrivals.poll();
			arrival.agent.notifyArrivalOnLinkByNonNetworkMode(arrival.arrivalLinkId);
			eventsManager.processEvent(arrival.event);
			arrival.agent.endLegAndComputeNextState(time);
			internalInterface.arrangeNextAgentState(arrival.agent);
		}
	}

	@Override
	public void onPrepareSim() {
		departures.clear();
		arrivals.clear();
	}

	@Override
	public void afterSim() {
		double time = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		Set<MobsimAgent> processedAgents = new HashSet<>();

		for (AgentDeparture departure : departures) {
			eventsManager
					.processEvent(new PersonStuckEvent(time, departure.agent.getId(), departure.departureLinkId, "pt"));
			processedAgents.add(departure.agent);
		}

		for (AgentArrival arrival : arrivals) {
			if (!processedAgents.contains(arrival.agent)) {
				eventsManager
						.processEvent(new PersonStuckEvent(time, arrival.agent.getId(), arrival.arrivalLinkId, "pt"));
			}
		}
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
}
