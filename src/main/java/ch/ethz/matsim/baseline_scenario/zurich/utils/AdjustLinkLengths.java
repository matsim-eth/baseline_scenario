package ch.ethz.matsim.baseline_scenario.zurich.utils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class AdjustLinkLengths {
	final private double minimumLinkLength;

	public AdjustLinkLengths(double minimumLinkLength) {
		this.minimumLinkLength = minimumLinkLength;
	}

	public void run(Network network) {
		for (Link link : network.getLinks().values()) {
			link.setLength(Math.max(minimumLinkLength, link.getLength()));
		}
	}
}
