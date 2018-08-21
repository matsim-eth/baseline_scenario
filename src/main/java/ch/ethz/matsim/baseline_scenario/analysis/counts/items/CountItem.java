package ch.ethz.matsim.baseline_scenario.analysis.counts.items;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

abstract public class CountItem {
	public String countStationId;
	public Id<Link> link;
	public Coord location;
	public int reference;
	public int simulation;

	public CountItem(Id<Link> link, int reference, Coord location, String countStationId) {
		this.reference = reference;
		this.location = location;
		this.link = link;
		this.countStationId = countStationId;
	}

	public void increase(int amount) {
		simulation += amount;
	}
	
	public void reset() {
		simulation = 0;
	}
}
