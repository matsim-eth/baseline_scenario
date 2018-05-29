package ch.ethz.matsim.baseline_scenario.mode_choice.modes.public_transit;

import ch.ethz.matsim.mode_choice.mnl.prediction.DefaultTripPrediction;

public class ASTRAPublicTransitTripPrediction extends DefaultTripPrediction {
	final private int numberOfLineSwitches;
	final private boolean isOnlyTransitWalk;
	final private double transferTime;
	final private double walkTime;

	public ASTRAPublicTransitTripPrediction(double travelTime, double travelDistance, double transferTime,
			double walkTime, int numberOfLinSwitches, boolean isOnlyTransitWalk) {
		super(travelTime, travelDistance);

		this.numberOfLineSwitches = numberOfLinSwitches;
		this.isOnlyTransitWalk = isOnlyTransitWalk;
		this.transferTime = transferTime;
		this.walkTime = walkTime;
	}

	public int getNumberOfLineSwitches() {
		return numberOfLineSwitches;
	}

	public boolean isOnlyTransitWalk() {
		return isOnlyTransitWalk;
	}

	public double getTransferTime() {
		return transferTime;
	}

	public double getWalkTime() {
		return walkTime;
	}
}
