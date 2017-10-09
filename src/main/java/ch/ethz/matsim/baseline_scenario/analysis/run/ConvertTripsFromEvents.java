package ch.ethz.matsim.baseline_scenario.analysis.run;

import java.io.IOException;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

import ch.ethz.matsim.baseline_scenario.analysis.CSVTripWriter;
import ch.ethz.matsim.baseline_scenario.analysis.TripItem;
import ch.ethz.matsim.baseline_scenario.analysis.listeners.TripListener;
import ch.ethz.matsim.baseline_scenario.analysis.readers.EventsTripReader;
import ch.ethz.matsim.baseline_scenario.analysis.utils.BaselineHomeActivityTypes;
import ch.ethz.matsim.baseline_scenario.analysis.utils.HomeActivityTypes;

public class ConvertTripsFromEvents {
	static public void main(String[] args) throws IOException {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(args[0]);
		
		StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
		HomeActivityTypes homeActivityTypes = new BaselineHomeActivityTypes();
		MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();
		
		TripListener tripListener = new TripListener(network, stageActivityTypes, homeActivityTypes, mainModeIdentifier);
		Collection<TripItem> trips = new EventsTripReader(tripListener).readTrips(args[1]);
		
		new CSVTripWriter(trips).write(args[2]);
	}
}
