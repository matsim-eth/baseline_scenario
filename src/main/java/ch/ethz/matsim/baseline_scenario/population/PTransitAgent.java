package ch.ethz.matsim.baseline_scenario.population;


import ch.ethz.matsim.baseline_scenario.transit.routing.EnrichedTransitRoute;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * Own implementation of boarding behavior. Note, that this behavior may be (more) inconsistent with the transit router as the default implementation.
 * (manserpa): made it compatible with EnrichedTransitRoute
 *
 * @author aneumann, manserpa
 */

public class PTransitAgent extends PersonDriverAgentImpl implements MobsimDriverPassengerAgent {

    private static final Logger log = Logger.getLogger(PTransitAgent.class);

    private final TransitSchedule transitSchedule;

    public static PTransitAgent createTransitAgent(Person p, Netsim simulation) {
        return new PTransitAgent(p, simulation);
    }

    private PTransitAgent(final Person p, final Netsim simulation) {
        super(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), simulation);
        this.transitSchedule = simulation.getScenario().getTransitSchedule();
    }

    @Override
    public boolean getExitAtStop(final TransitStopFacility stop) {
        EnrichedTransitRoute route = (EnrichedTransitRoute) getCurrentLeg().getRoute();
        return route.getEgressStopId().equals(stop.getId());
    }

    @Override
    public boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute, final List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle) {
        EnrichedTransitRoute route = (EnrichedTransitRoute) getCurrentLeg().getRoute();
        Id<TransitStopFacility> accessStopId = route.getAccessStopId();
        Id<TransitStopFacility> egressStopId = route.getEgressStopId();

        if(containsId(stopsToCome, egressStopId))	{
            if (route.getTransitRouteId().toString().equalsIgnoreCase(transitRoute.getId().toString())) {
                LinkedList<TransitRouteStop> tempStopsToCome = new LinkedList<>(stopsToCome);
                tempStopsToCome.removeLast();
                boolean egressStopFound = false;
                for (TransitRouteStop stop : tempStopsToCome) {
                    if (egressStopId.equals(stop.getStopFacility().getId())) {
                        egressStopFound = true;
                    } else if (accessStopId.equals(stop.getStopFacility().getId())) {
                        // route is looping - decide whether to board now or later
                        if (egressStopFound) {
                            // egress stop found - so the agent will be able to reach its destination before the vehicle returns to this stop
                            // boarding now should be faster
                            return true;
                        } else {
                            // egress stop not found - the vehicle will return before reaching the agent's destination
                            // boarding now or the next the vehicle passes by will not change the arrival time
                            // although people tend to board the first vehicle arriving, lines looping may impose extra costs, e.g. increased ticket costs due to more kilometer or hours traveled
                            // thus, board as late as possible
                            return false;
                        }
                    }
                }
                // nothing wrong, e.g. not looping and it's the route planned - just board
                return true;
            }

            if (this.transitSchedule.getTransitLines().get(route.getTransitLineId()) == null) {
                // agent is still on an old line, which probably went bankrupt - enter anyway
                return true;
            }

            TransitRoute transitRoutePlanned = this.transitSchedule.getTransitLines().get(route.getTransitLineId()).getRoutes().get(route.getTransitRouteId());
            if (transitRoutePlanned == null) {
                // agent is still on an old route, which probably got dropped - enter anyway
                return true;
            }

            TransitRoute transitRouteOffered = this.transitSchedule.getTransitLines().get(line.getId()).getRoutes().get(transitRoute.getId());

            double travelTimePlanned = getArrivalOffsetFromRoute(transitRoutePlanned, egressStopId) - getDepartureOffsetFromRoute(transitRoutePlanned, accessStopId);
            double travelTimeOffered = getArrivalOffsetFromRoute(transitRouteOffered, egressStopId) - getDepartureOffsetFromRoute(transitRouteOffered, accessStopId);

            if (travelTimeOffered <= travelTimePlanned) {
                // transit route offered is faster the the one planned - enter
                return true;
            }
        }

        return false;
    }

    private double getArrivalOffsetFromRoute(TransitRoute transitRoute, Id<TransitStopFacility> egressStopId) {
        for (TransitRouteStop routeStop : transitRoute.getStops()) {
            if (egressStopId.equals(routeStop.getStopFacility().getId())) {
                return routeStop.getArrivalOffset();
            }
        }

        //log.error("Stop " + egressStopId + " not found in route " + transitRoute.getId());
        // returning what???
        return Double.MAX_VALUE;
    }

    private double getDepartureOffsetFromRoute(TransitRoute transitRoute, Id<TransitStopFacility> accessStopId) {
        for (TransitRouteStop routeStop : transitRoute.getStops()) {
            if (accessStopId.equals(routeStop.getStopFacility().getId())) {
                return routeStop.getDepartureOffset();
            }
        }

        //log.error("Stop " + accessStopId + " not found in route " + transitRoute.getId());
        // returning what???
        return -Double.MAX_VALUE;
    }


    private Leg getCurrentLeg() {
        PlanElement currentPlanElement = this.getCurrentPlanElement();
        return (Leg) currentPlanElement;
    }

    private boolean containsId(List<TransitRouteStop> stopsToCome, Id<TransitStopFacility> egressStopId) {
        for (TransitRouteStop stop : stopsToCome) {
            if (egressStopId.equals(stop.getStopFacility().getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public double getWeight() {
        return 1.0;
    }

    @Override
    public Id<TransitStopFacility> getDesiredAccessStopId() {
        Leg leg = getCurrentLeg();
        if (!(leg.getRoute() instanceof EnrichedTransitRoute)) {
            log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + getId().toString());
            log.info("route: "
                    + leg.getRoute().getClass().getCanonicalName()
                    + " "
                    + leg.getRoute().getRouteDescription());
            return null;
        } else {
            EnrichedTransitRoute route = (EnrichedTransitRoute) leg.getRoute();
            return route.getAccessStopId();
        }
    }

    @Override
    public Id<TransitStopFacility> getDesiredDestinationStopId() {
        Leg leg = getCurrentLeg();
        if (!(leg.getRoute() instanceof EnrichedTransitRoute)) {
            log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + getId().toString());
            log.info("route: "
                    + leg.getRoute().getClass().getCanonicalName()
                    + " "
                    + leg.getRoute().getRouteDescription());
            return null;
        } else {
            EnrichedTransitRoute route = (EnrichedTransitRoute) leg.getRoute();
            return route.getEgressStopId();
        }
    }
}