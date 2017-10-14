package ch.ethz.matsim.baseline_scenario.analysis.counts.readers;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.utils.IndexBuilder;

public class HourlySimulationCountsListener implements SimulationCountsListener {
	final private Map<Id<Link>, List<HourlyCountItem>> items;
	
	public HourlySimulationCountsListener(Collection<HourlyCountItem> items) {
		this.items = IndexBuilder.buildHourlyIndex(items);
	}

	@Override
	public void reset(int iteration) {}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		int hour = (int) (event.getTime() / 3600.0);
		Id<Link> linkId = event.getLinkId();
		
		if (items.containsKey(linkId) && hour >= 0 && hour < 24) {
			items.get(linkId).get(hour).increase(1);
		}
	}
}
