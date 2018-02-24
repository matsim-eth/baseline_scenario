package ch.ethz.matsim.baseline_scenario.zurich.cutter.network;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public interface MinimumNetworkFinder {
	Set<Id<Link>> run(Set<Id<Link>> links);
}
