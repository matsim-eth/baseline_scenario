package ch.ethz.matsim.baseline_scenario.mode_choice.modes.walk;

import ch.ethz.matsim.baseline_scenario.scoring.ASTRAScoringParameters;
import ch.ethz.matsim.mode_choice.ModeChoiceTrip;
import ch.ethz.matsim.mode_choice.mnl.ModeChoiceAlternative;
import ch.ethz.matsim.mode_choice.mnl.prediction.FixedSpeedPredictor;
import ch.ethz.matsim.mode_choice.mnl.prediction.PredictionCache;
import ch.ethz.matsim.mode_choice.mnl.prediction.TripPrediction;

public class WalkAlternative implements ModeChoiceAlternative {
	final private ASTRAScoringParameters parameters;
	final private FixedSpeedPredictor predictor;
	final private PredictionCache cache;
	
	public WalkAlternative(ASTRAScoringParameters parameters, FixedSpeedPredictor predictor, PredictionCache cache) {
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
		
		utility += parameters.alphaWalk;
		utility += parameters.betaTravelTimeWalk * prediction.getPredictedTravelTime() / 60.0;
		
		return utility;
	}

	@Override
	public boolean isChainMode() {
		return false;
	}
}
