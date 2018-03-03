package ch.ethz.matsim.baseline_scenario.analysis.transit.run;

import java.io.IOException;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

import ch.ethz.matsim.baseline_scenario.analysis.transit.CSVTransitTripWriter;
import ch.ethz.matsim.baseline_scenario.analysis.transit.TransitTripItem;
import ch.ethz.matsim.baseline_scenario.analysis.transit.listeners.TransitTripListener;
import ch.ethz.matsim.baseline_scenario.analysis.transit.readers.EventsTransitTripReader;

public class ConvertTransitTripsFromEvents {
	static public void main(String[] args) throws IOException {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(args[0]);

		StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);

		TransitTripListener tripListener = new TransitTripListener(stageActivityTypes, network);
		Collection<TransitTripItem> trips = new EventsTransitTripReader(tripListener).readTrips(args[1]);

		new CSVTransitTripWriter(trips).write(args[2]);
	}
}
