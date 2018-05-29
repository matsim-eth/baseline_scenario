package ch.ethz.matsim.baseline_scenario.mode_choice.modes.public_transit;

import ch.ethz.matsim.baseline_scenario.scoring.ASTRAScoringParameters;
import ch.ethz.matsim.mode_choice.ModeChoiceTrip;
import ch.ethz.matsim.mode_choice.mnl.ModeChoiceAlternative;
import ch.ethz.matsim.mode_choice.mnl.prediction.PredictionCache;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class PublicTransportAlternative implements ModeChoiceAlternative {
	final private ASTRAScoringParameters parameters;
	final private ASTRAPublicTransitTripPredictor predictor;
	final private PredictionCache cache;
	final private ObjectAttributes personAttributes;

	public PublicTransportAlternative(ASTRAScoringParameters parameters, ASTRAPublicTransitTripPredictor predictor,
                                      PredictionCache cache, ObjectAttributes personAttributes) {
		this.parameters = parameters;
		this.predictor = predictor;
		this.cache = cache;
		this.personAttributes = personAttributes;
	}

	@Override
	public boolean isFeasible(ModeChoiceTrip trip) {
		double crowflyDistance = CoordUtils.calcEuclideanDistance(trip.getOriginLink().getCoord(),
				trip.getDestinationLink().getCoord());

		if (crowflyDistance <= 200) {
			return false;
		}

		ASTRAPublicTransitTripPrediction prediction = (ASTRAPublicTransitTripPrediction) cache.get(trip);

		if (prediction == null) {
			prediction = (ASTRAPublicTransitTripPrediction) predictor.predictTrip(trip);
			cache.put(trip, prediction);
		}

		if (prediction.isOnlyTransitWalk()) {
			return false;
		}

		return true;
	}

	@Override
	public double estimateUtility(ModeChoiceTrip trip) {
		if (!isFeasible(trip)) {
			return -100000;
		}

		ASTRAPublicTransitTripPrediction prediction = (ASTRAPublicTransitTripPrediction) cache.get(trip);

		if (prediction == null) {
			prediction = (ASTRAPublicTransitTripPrediction) predictor.predictTrip(trip);
			cache.put(trip, prediction);
		}

		double utility = 0.0;

		double crowflyDistance = CoordUtils.calcEuclideanDistance(trip.getOriginLink().getCoord(),
				trip.getDestinationLink().getCoord());

		double cost = 0.0;

		if (personAttributes.getAttribute(trip.getPerson().getId().toString(), "season_ticket") == null) {
			cost = parameters.getPtCostPerKm_Becker(crowflyDistance * 1e-3);
			if (cost * crowflyDistance < 2.7)
				cost = 2.7 / crowflyDistance;
		} else if (((String) personAttributes.getAttribute(trip.getPerson().getId().toString(), "season_ticket"))
				.contains("Generalabo")) {
			cost = 0.0;
		} else if (((String) personAttributes.getAttribute(trip.getPerson().getId().toString(), "season_ticket"))
				.contains("Verbund")) {

			cost = parameters.getPtCostPerKm_zvv(crowflyDistance * 1e-3);
		} else if (((String) personAttributes.getAttribute(trip.getPerson().getId().toString(), "season_ticket"))
				.contains("Halbtax")) {
			cost = parameters.getPtCostPerKm_Becker(crowflyDistance * 1e-3) * 0.5;

			if (cost * crowflyDistance < 2.3)
				cost = 2.3 / crowflyDistance;
		} else {
			cost = parameters.getPtCostPerKm_Becker(crowflyDistance * 1e-3);
			if (cost * crowflyDistance < 2.3)
				cost = 2.3 / crowflyDistance;
		}

		utility += parameters.alphaPt;
		utility += parameters.betaTravelTimePt * prediction.getPredictedTravelTime() / 60.0;
		utility += parameters.betaTransfers * prediction.getNumberOfLineSwitches();
		utility += parameters.betaWaitPt * prediction.getTransferTime() / 60.0;
		utility += parameters.betaWalkPt * prediction.getWalkTime() / 60.0;
		utility += parameters.getCostBeta(prediction.getPredictedTravelDistance() * 1e-3, parameters.averageIncome)
				* cost;
		return utility;
	}

	@Override
	public boolean isChainMode() {
		return false;
	}
}
