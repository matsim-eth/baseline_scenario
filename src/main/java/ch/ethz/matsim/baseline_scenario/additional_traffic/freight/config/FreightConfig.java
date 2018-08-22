package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.config;

import java.util.HashMap;
import java.util.Map;

public class FreightConfig {
    public int randomSeed = 0;
    public String cumulativeDepartureProbabilityPath;
    public String zoneCentroidsPath;
    public String borderFacilitiesPath;
    public int zoneRadius = 1000;
    public Map<String, String> freightVehiclesPaths = createDefault();

    private Map<String, String> createDefault() {
        Map<String,String> map = new HashMap<String,String>();
        map.put("freightType1", "path1");
        map.put("freightType2", "path2");
        return map;
    }
}
