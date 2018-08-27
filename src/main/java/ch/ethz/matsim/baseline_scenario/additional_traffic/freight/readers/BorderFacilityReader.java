package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.ZoneItem;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
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

    public Map<Integer, ZoneItem> read(String path) throws IOException {
        log.info("Trying to load border facilities for international zones from " + path);
        Map<Integer, ZoneItem> borderFacilities = new HashMap<>();

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
                String name = row.get(header.indexOf("name"));
                Id<ActivityFacility> facilityId = Id.create(row.get(header.indexOf("facility_id")), ActivityFacility.class);
                if (!facilities.getFacilities().containsKey(facilityId)) {
                    log.error("Facilities does not contain " + facilityId.toString() +
                            " Make sure that facilities include border facilities!");
                }

                ActivityFacility facility = facilities.getFacilities().get(facilityId);
                Coord coord = facility.getCoord();

                Set<ActivityFacility> facilities = new HashSet<>();
                facilities.add(facility);

                borderFacilities.putIfAbsent(zoneId, new ZoneItem(zoneId, name, coord, facilities));
                counter.incCounter();
            }
        }

        reader.close();
        counter.printCounter();

        return borderFacilities;
    }
}
