package ch.ethz.matsim.baseline_scenario.analysis.transit.run;

import java.io.IOException;
import java.util.*;

import ch.ethz.matsim.baseline_scenario.utils.CSVReader;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import ch.ethz.matsim.baseline_scenario.analysis.transit.CSVTransitTripWriter;
import ch.ethz.matsim.baseline_scenario.analysis.transit.TransitTripItem;
import ch.ethz.matsim.baseline_scenario.analysis.transit.listeners.TransitTripListener;
import ch.ethz.matsim.baseline_scenario.analysis.transit.readers.EventsTransitTripReader;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class ConvertTransitTripsFromEvents {
	static public void main(String[] args) throws IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[0]);
		new TransitScheduleReader(scenario).readFile(args[1]);
		// for the reference case...
		HashMap<Id<TransitLine>, Set<Id<TransitRoute>>> removedLines = readRemovedLines(args[2]);

		StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);

		TransitTripListener tripListener = new TransitTripListener(stageActivityTypes,
				scenario.getNetwork(), scenario.getTransitSchedule(), removedLines);
		Collection<TransitTripItem> trips = new EventsTransitTripReader(tripListener).readTrips(args[3]);

		new CSVTransitTripWriter(trips).write(args[4]);
	}

	public static HashMap<Id<TransitLine>, Set<Id<TransitRoute>>> readRemovedLines(String path) {
		HashMap<Id<TransitLine>, Set<Id<TransitRoute>>> removedRoutes = new HashMap<>();

		String[] columns = {"line", "route","mode"};
		try (CSVReader reader = new CSVReader(columns, path, ";")) {
			Map<String, String> row = reader.readLine(); // header
			while ((row = reader.readLine()) != null) {

				Id<TransitLine> line = Id.create(row.get("line"), TransitLine.class);
				removedRoutes.putIfAbsent(line, new HashSet<>());

				Id<TransitRoute> route = Id.create(row.get("route"), TransitRoute.class);
				removedRoutes.get(line).add(route);
			}
		}
		catch(IOException ex){
			System.out.println (ex.toString());
		}

		return removedRoutes;
	}
}
