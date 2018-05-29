package ch.ethz.matsim.baseline_scenario.mode_choice.modes.public_transit;

import ch.ethz.matsim.baseline_scenario.transit.routing.EnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.EnrichedTransitRouter;
import ch.ethz.matsim.mode_choice.ModeChoiceTrip;
import ch.ethz.matsim.mode_choice.mnl.prediction.TripPrediction;
import ch.ethz.matsim.mode_choice.mnl.prediction.TripPredictor;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.LinkWrapperFacility;

import java.util.List;

public class ASTRAPublicTransitTripPredictor implements TripPredictor {
	final private EnrichedTransitRouter transitRouter;

	public ASTRAPublicTransitTripPredictor(EnrichedTransitRouter transitRouter) {
		this.transitRouter = transitRouter;
	}

	@Override
	public TripPrediction predictTrip(ModeChoiceTrip trip) {
		List<Leg> legs = transitRouter.calculateRoute(new LinkWrapperFacility(trip.getOriginLink()),
				new LinkWrapperFacility(trip.getDestinationLink()), trip.getDepartureTime(), trip.getPerson());

		boolean atLeastOnePtLeg = false;
		int numberOfLineSwitches = -1;

		double transferTime = 0.0;

		double travelTime = 0.0;
		double travelDistance = 0.0;

		double walkTime = 0.0;

		for (Leg leg : legs) {
			if (leg.getMode().equals("pt")) {
				numberOfLineSwitches++;

				EnrichedTransitRoute route = (EnrichedTransitRoute) leg.getRoute();

				travelTime += route.getInVehicleTime();
				travelDistance += route.getDistance();

				if (!atLeastOnePtLeg) {
					// This is the first PT leg. Only score 60s of waiting time here!
					transferTime += Math.min(route.getWaitingTime(), 60.0);
				} else {
					transferTime += route.getWaitingTime();
				}

				atLeastOnePtLeg = true;
			} else if (leg.getMode().contains("walk")) {
				walkTime += leg.getTravelTime();
			} else {
				throw new IllegalStateException("Can only enrich pt and *_walk legs");
			}
		}

		numberOfLineSwitches = Math.max(0, numberOfLineSwitches);

		return new ASTRAPublicTransitTripPrediction(travelTime, travelDistance, transferTime, walkTime,
				numberOfLineSwitches, !atLeastOnePtLeg);
	}
}
