package ch.ethz.matsim.baseline_scenario.analysis.counts.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;

public class IndexBuilder {
	final static private Logger logger = Logger.getLogger(IndexBuilder.class);
	
	public static Map<Id<Link>, DailyCountItem> buildDailyIndex(Collection<DailyCountItem> items) {
		Map<Id<Link>, DailyCountItem> index = new HashMap<>();

		for (DailyCountItem item : items) {
			if (index.containsKey(item.link)) {
				logger.warn(String.format("Multiple count records for link %s, ignoring.", item.link.toString()));
			} else {
				index.put(item.link, item);
			}
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
			
			if (linkBin.get(item.hour) == null) {
				linkBin.set(item.hour, item);
			} else {
				logger.warn(String.format("Multiple count records for link %s at %s, ignoring.", item.link.toString(), Time.writeTime(item.hour * 3600.0)));
			}
		}

		return index;
	}
}
