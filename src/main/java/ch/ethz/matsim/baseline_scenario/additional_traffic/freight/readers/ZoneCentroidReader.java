package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils.Zone2FacilitiesAssigner;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toCH1903LV03Plus;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class ZoneCentroidReader {
    private static final Logger log = Logger.getLogger(ZoneCentroidReader.class);
    private final static String DELIMITER = ";";
    private final static CoordinateTransformation transformation = new CH1903LV03toCH1903LV03Plus();
    private ActivityFacilities facilities;
    private int radius;

    public ZoneCentroidReader(ActivityFacilities facilities, int radius) {
        this.facilities = facilities;
        this.radius = radius;
    }

    public Map<Integer, Set<ActivityFacility>> read(String path) throws IOException {
        log.info("Trying to load zone centroids for national zones from" + path);
        Map<Integer, Set<ActivityFacility>> zone2facilities = new HashMap<>();
        Zone2FacilitiesAssigner zone2FacilitiesAssigner = new Zone2FacilitiesAssigner(facilities);

        // read zone centroids and assign all facilities close to centroid
        Counter counter = new Counter(" zone # ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(DELIMITER));

            if (header == null) {
                header = row;
            } else {
                int zoneId = Integer.parseInt(row.get(header.indexOf("zone_id")));
                double x = Double.parseDouble(row.get(header.indexOf("x")));
                double y = Double.parseDouble(row.get(header.indexOf("y")));
                Coord coord = transformation.transform(new Coord(x, y));
                zone2facilities.putIfAbsent(zoneId, zone2FacilitiesAssigner.assign(coord, radius));

                counter.incCounter();
            }
        }

        reader.close();
        counter.printCounter();

        return zone2facilities;
    }
}
