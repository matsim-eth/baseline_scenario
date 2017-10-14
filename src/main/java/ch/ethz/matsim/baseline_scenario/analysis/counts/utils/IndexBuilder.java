package ch.ethz.matsim.baseline_scenario.analysis.counts.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;

public class IndexBuilder {
	public static Map<Id<Link>, DailyCountItem> buildDailyIndex(Collection<DailyCountItem> items) {
		Map<Id<Link>, DailyCountItem> index = new HashMap<>();

		for (DailyCountItem item : items) {
			index.put(item.link, item);
		}

		return index;
	}

	public static Map<Id<Link>, List<HourlyCountItem>> buildHourlyIndex(Collection<HourlyCountItem> items) {
		Map<Id<Link>, List<HourlyCountItem>> index = new HashMap<>();

		for (HourlyCountItem item : items) {
			List<HourlyCountItem> linkBin = index.get(item.link);
			
			if (linkBin == null) {
				linkBin = new ArrayList<>(Collections.nCopies(24, (HourlyCountItem) null));
				index.put(item.link, linkBin);
			}
			
			linkBin.set(item.hour, item);
		}

		return index;
	}
}
