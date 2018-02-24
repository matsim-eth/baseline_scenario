package ch.ethz.matsim.baseline_scenario.zurich.cutter.schedule;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleWriterV1;

import ch.ethz.matsim.baseline_scenario.zurich.extent.CircularScenarioExtent;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class RunScheduleCutter {
	static public void main(String[] args) {
		String transitScheduleInputPath = args[0];
		String transitVehiclesInputPath = args[1];
		String networkInputPath = args[2];
		String transitScheduleOutputPath = args[3];
		String transitVehiclesOutputPath = args[4];

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkInputPath);
		new TransitScheduleReader(scenario).readFile(transitScheduleInputPath);
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(transitVehiclesInputPath);

		Coord bellevue = new Coord(2683253.0, 1246745.0);
		ScenarioExtent extent = new CircularScenarioExtent(scenario.getNetwork(), bellevue, 30000.0);

		StopSequenceCrossingPointFinder crossingPointFinder = new DefaultStopSequenceCrossingPointFinder(extent);

		new TransitScheduleCutter(extent, crossingPointFinder).run(scenario.getTransitSchedule());
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(transitScheduleOutputPath);

		new TransitVehiclesCutter(scenario.getTransitSchedule()).run(scenario.getTransitVehicles());
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(transitVehiclesOutputPath);
	}
}
