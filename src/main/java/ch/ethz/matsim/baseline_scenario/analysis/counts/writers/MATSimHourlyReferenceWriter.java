package ch.ethz.matsim.baseline_scenario.analysis.counts.writers;

import java.io.IOException;
import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;

public class MATSimHourlyReferenceWriter {
	final private Collection<HourlyCountItem> items;

	public MATSimHourlyReferenceWriter(Collection<HourlyCountItem> items) {
		this.items = items;
	}

	public void write(String outputPath) throws IOException {
		Counts<Link> counts = new Counts<Link>();
		counts.setName("reference");
		counts.setYear(2017);
		
		for (HourlyCountItem item : items) {

			Count<Link> count;
			if (counts.getCounts().containsKey(item.link)) {
				count = counts.getCount(item.link);
			} else {
				count = counts.createAndAddCount(item.link, item.link.toString());
			}
			
			count.setCoord(item.location);
			count.createVolume(item.hour + 1, item.reference);
		}
		
		new CountsWriter(counts).write(outputPath);
	}
}
