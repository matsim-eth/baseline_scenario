package ch.ethz.matsim.baseline_scenario.analysis.listeners;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.baseline_scenario.analysis.TripItem;

public class TripListenerItem extends TripItem {
	public String startPurpose;
	public List<PlanElement> elements = new LinkedList<>();
	public List<Id<Link>> route = new LinkedList<>();
	
	public TripListenerItem(Coord origin, double startTime, String startPurpose) {
		super(origin, null, startTime, Double.NaN, Double.NaN, "unknown", "unknown", false);
		this.startPurpose = startPurpose;
	}
}
