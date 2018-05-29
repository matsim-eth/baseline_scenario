package ch.ethz.matsim.baseline_scenario.scoring;

public class ASTRAScoringParameters {
	public double betaCost = -0.0382;
	public double betaAgeBike = -0.0496;
	public double betaTransfers = -0.444;
	public double betaWaitAv = -0.135;
	public double betaWaitPt = -0.00269;
	public double betaWalkPt = -0.00482;

	public double alphaBike = -0.399;
	public double alphaCar = 0.673;
	public double alphaPav = 0.120;
	public double alphaPt = 0.00;
	public double alphaSav = -0.213;
	public double alphaWalk = -0.103;

	public double lambdaDistanceCost = -0.898;
	public double lambdaIncome = 0.778;

	public double betaTravelTimeBike = -0.0427;
	public double betaTravelTimeCar = -0.0153;
	public double betaTravelTimePav = -0.0102;
	public double betaTravelTimePt = -0.00780;
	public double betaTravelTimeSav = -0.0132;
	public double betaTravelTimeWalk = -0.0534;

	public double costPerKmCar = 0.268;
	public double costPerKmPt = 0.36;
	public double costPerKmSav = 0.0;
	public double costPerKmPav = 0.0;

	public double averageIncome = 7722.22;
	public double averageDistanceKm = 42.877;

	public double onlyTransferWalkPenalty = -10000.0;
	public double avOutsideOfOperatingAreaPenalty = -10000.0;
	public double noCarPossiblePenalty = -10000.0;
	public double carAndPravPenalty = -10000.0;
	public double invalidPravPlanPenalty = -10000.0;

	public double minimumCostPt = 2.6;
	public double minimumCostCar = 1.2;

	public double parkingCostPerLeg = 0.0;
	public double parkingSearchPenaltyMinutes = 0.0;

	public double carAccessEgressWalkTimeMin = 5.0;

	public double privateAVWaitingTimeMin = 0.0;

	public double stuckPenalty = -2000.0; // Double.NEGATIVE_INFINITY;

	public enum PtCostStructure {
		Becker, Linear
	}

	public PtCostStructure ptCostStructure = PtCostStructure.Linear;

	public double getPtCostPerKm_Becker(double distance) {
		double cost = 0.0;

		if (distance <= 1.0) {
			cost = 4.51;
		} else if (distance <= 2.0) {
			cost = 1.799;
		} else if (distance <= 5.0) {
			cost = 1.31;
		} else if (distance <= 10.0) {
			cost = 0.878;
		} else if (distance <= 20.0) {
			cost = 0.685;
		} else if (distance <= 30.0) {
			cost = 0.619;
		} else if (distance <= 40.0) {
			cost = 0.676;
		} else if (distance <= 50.0) {
			cost = 0.655;
		} else {
			cost = 0.5898;
		}

		return cost;
	}

	public double getPtCostPerKm_zvv(double distance) {

		if (distance <= 30.0)
			return 0;
		else if (distance <= 40)
			return 0.437;
		else if (distance <= 50)
			return 0.564;
		else
			return 0.491;
	}

	public double getPtCost(double distance) {
		if (ptCostStructure == PtCostStructure.Becker) {
			return getPtCostPerKm_Becker(distance) * distance;
		} else {
			return Math.max(minimumCostPt, costPerKmPt * distance);
		}
	}

	public double getCarCost(double distance) {
		return Math.max(minimumCostCar, costPerKmCar * distance);
	}

	public double getPrivateAVCost(double distance) {
		return Math.max(minimumCostCar, costPerKmCar * distance);
	}

	public double getCostBeta(double distance, double income) {
		distance = Math.max(distance, 0.001);
		return betaCost * Math.pow(income / averageIncome, lambdaIncome)
				* Math.pow(distance / averageDistanceKm, lambdaDistanceCost);
	}
}
