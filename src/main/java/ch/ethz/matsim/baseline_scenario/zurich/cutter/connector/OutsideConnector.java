package ch.ethz.matsim.baseline_scenario.zurich.cutter.connector;

import org.matsim.api.core.v01.network.Network;
import org.matsim.facilities.ActivityFacilities;

public interface OutsideConnector {

	void run(ActivityFacilities facilities, Network network, Network roadNetwork);

}