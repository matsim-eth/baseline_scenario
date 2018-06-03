package ch.ethz.matsim.baseline_scenario.analysis.counts.readers;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.CountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.utils.IndexBuilder;

public class DailySimulationCountsListener implements SimulationCountsListener {
	final private Map<Id<Link>, DailyCountItem> items;

	public DailySimulationCountsListener(Collection<DailyCountItem> items) {
		items.forEach(CountItem::reset);
		this.items = IndexBuilder.buildDailyIndex(items);
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		int hour = (int) (event.getTime() / 3600.0);
		Id<Link> linkId = event.getLinkId();

		if (items.containsKey(linkId) && hour >= 0 && hour < 24) {
			items.get(linkId).increase(1);
		}
	}
}
