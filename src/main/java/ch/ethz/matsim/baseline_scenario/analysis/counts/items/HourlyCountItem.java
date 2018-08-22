package ch.ethz.matsim.baseline_scenario.analysis.counts.items;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class HourlyCountItem extends CountItem {
	public int hour;
	
	public HourlyCountItem(Id<Link> linkId, int hour, int reference, Coord location, String countStationId) {
		super(linkId, reference, location, countStationId);
		this.hour = hour;
	}
}
