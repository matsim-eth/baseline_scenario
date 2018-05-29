package ch.ethz.matsim.baseline_scenario.mode_choice.modes.bike;

import ch.ethz.matsim.mode_choice.ModeChoiceTrip;
import ch.ethz.matsim.mode_choice.mnl.ModeChoiceAlternative;
import ch.ethz.matsim.mode_choice.mnl.prediction.FixedSpeedPredictor;
import ch.ethz.matsim.mode_choice.mnl.prediction.PredictionCache;
import ch.ethz.matsim.mode_choice.mnl.prediction.TripPrediction;
import ch.ethz.matsim.baseline_scenario.scoring.ASTRAScoringParameters;

public class BikeAlternative implements ModeChoiceAlternative {
	final private ASTRAScoringParameters parameters;
	final private FixedSpeedPredictor predictor;
	final private PredictionCache cache;

	public BikeAlternative(ASTRAScoringParameters parameters, FixedSpeedPredictor predictor, PredictionCache cache) {
		this.parameters = parameters;
		this.predictor = predictor;
		this.cache = cache;
	}
	
	@Override
	public boolean isFeasible(ModeChoiceTrip trip) {
		return true;
	}

	@Override
	public double estimateUtility(ModeChoiceTrip trip) {
		TripPrediction prediction = cache.get(trip);

		if (prediction == null) {
			prediction = predictor.predictTrip(trip);
			cache.put(trip, prediction);
		}

		double utility = 0.0;

		int age = (int) trip.getPerson().getAttributes().getAttribute("age");

		if (age >= 18) {
			utility += parameters.betaAgeBike * (age - 18);
		}

		utility += parameters.alphaBike;
		utility += parameters.betaTravelTimeBike * prediction.getPredictedTravelTime() / 60.0;

		return utility;
	}

	@Override
	public boolean isChainMode() {
		return true;
	}
}
