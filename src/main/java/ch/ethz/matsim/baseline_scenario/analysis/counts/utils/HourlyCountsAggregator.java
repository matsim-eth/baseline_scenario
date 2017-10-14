package ch.ethz.matsim.baseline_scenario.analysis.counts.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;

public class HourlyCountsAggregator {
	public Collection<DailyCountItem> aggregate(Collection<HourlyCountItem> hourlyItems) {
		Map<Id<Link>, DailyCountItem> dailyItems = new HashMap<>();
		
		for (HourlyCountItem hourlyItem : hourlyItems) {
			DailyCountItem dailyItem = dailyItems.get(hourlyItem.link);
			
			if (dailyItem == null) {
				dailyItem = new DailyCountItem(hourlyItem.link, 0, hourlyItem.location);
				dailyItems.put(dailyItem.link, dailyItem);
			}
			
			dailyItem.reference += hourlyItem.reference;
			dailyItem.simulation += hourlyItem.simulation;
		}
		
		return new ArrayList<>(dailyItems.values());		
	}
}
