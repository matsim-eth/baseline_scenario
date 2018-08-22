package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class BorderFacilityReader {
    private static final Logger log = Logger.getLogger(BorderFacilityReader.class);
    private final static String DELIMITER = ";";
    private ActivityFacilities facilities;

    public BorderFacilityReader(ActivityFacilities facilities) {
        this.facilities = facilities;
    }

    public Map<Integer, Set<ActivityFacility>> read(String path) throws IOException {
        log.info("Trying to load border facilities for international zones from " + path);
        Map<Integer, Set<ActivityFacility>> borderFacilities = new HashMap<>();

        // these are the international zones without centroids;
        // the id of the appropriate border facility is specified.
        Counter counter = new Counter(" international zone # ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(DELIMITER));

            if (header == null) {
                header = row;
            } else {
                int zoneId = Integer.parseInt(row.get(header.indexOf("zone_id")));
                borderFacilities.putIfAbsent(zoneId, new HashSet<>());
                borderFacilities.get(zoneId).add(facilities.getFacilities()
                        .get(Id.create(row.get(header.indexOf("border_facility_id")), ActivityFacility.class)));
                counter.incCounter();
            }
        }

        reader.close();
        counter.printCounter();

        return borderFacilities;
    }
}
