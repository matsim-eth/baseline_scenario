package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.ZoneItem;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

import java.io.IOException;
import java.util.Map;

public class RunBorderFacilityReader {
    public static void main(String[] args) throws IOException {
        String facilitiesPath = args[0];
        String borderFacilitiesPath = args[1];

        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesPath);
        Map<Integer, ZoneItem> map = new BorderFacilityReader(scenario.getActivityFacilities()).read(borderFacilitiesPath);
        System.exit(1);
    }
}
