package ch.ethz.matsim.baseline_scenario.mode_choice.modes.car;

import ch.ethz.matsim.baseline_scenario.scoring.ASTRAScoringParameters;
import ch.ethz.matsim.mode_choice.ModeChoiceTrip;
import ch.ethz.matsim.mode_choice.mnl.ModeChoiceAlternative;
import ch.ethz.matsim.mode_choice.mnl.prediction.NetworkPathPredictor;
import ch.ethz.matsim.mode_choice.mnl.prediction.PredictionCache;
import ch.ethz.matsim.mode_choice.mnl.prediction.TripPrediction;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class CarAlternative implements ModeChoiceAlternative {
	final private ASTRAScoringParameters parameters;
	final private NetworkPathPredictor predictor;
	final private PredictionCache cache;

	public CarAlternative(ASTRAScoringParameters parameters, NetworkPathPredictor predictor, PredictionCache cache) {
		this.parameters = parameters;
		this.predictor = predictor;
		this.cache = cache;
	}
	
	@Override
	public boolean isFeasible(ModeChoiceTrip trip) {
		if (PersonUtils.getCarAvail(trip.getPerson()).equals("never")) {
			return false;
		}
		
		if (!PersonUtils.hasLicense(trip.getPerson())) {
			return false;
		}
		
		double crowflyDistance = CoordUtils.calcEuclideanDistance(trip.getOriginLink().getCoord(),
				trip.getDestinationLink().getCoord());

		if (crowflyDistance <= 200) {
			return false;
		}
		
		return true;
	}

	@Override
	public double estimateUtility(ModeChoiceTrip trip) {
		if (!isFeasible(trip)) {
			return parameters.noCarPossiblePenalty;
		}

		TripPrediction prediction = cache.get(trip);

		if (prediction == null) {
			prediction = predictor.predictTrip(trip);
			cache.put(trip, prediction);
		}

		double utility = 0.0;
		double cost = parameters.getCarCost(prediction.getPredictedTravelDistance() * 1e-3)
				+ parameters.parkingCostPerLeg;

		utility += parameters.alphaCar;
		utility += parameters.betaTravelTimeCar
				* (prediction.getPredictedTravelTime() / 60.0 + parameters.parkingSearchPenaltyMinutes);
		utility += parameters.getCostBeta(prediction.getPredictedTravelDistance() * 1e-3, parameters.averageIncome)
				* cost;
		
		utility += parameters.betaTravelTimeWalk * parameters.carAccessEgressWalkTimeMin;

		return utility;
	}

	@Override
	public boolean isChainMode() {
		return true;
	}
}
