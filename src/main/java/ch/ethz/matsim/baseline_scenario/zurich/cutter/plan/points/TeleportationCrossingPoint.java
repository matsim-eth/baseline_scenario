package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points;

import org.matsim.api.core.v01.Coord;

public class TeleportationCrossingPoint {
	final public Coord coord;
	final public double time;
	final public boolean isOutgoing;

	public TeleportationCrossingPoint(Coord coord, double time, boolean isOutgoing) {
		this.coord = coord;
		this.time = time;
		this.isOutgoing = isOutgoing;
	}
}
