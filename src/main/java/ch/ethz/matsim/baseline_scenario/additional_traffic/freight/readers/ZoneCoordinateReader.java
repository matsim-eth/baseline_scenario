package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils.Zone2FacilitiesAssigner;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toCH1903LV03Plus;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ZoneCoordinateReader {
    private final static String DELIMITER = ";";
    private final static CoordinateTransformation transformation = new CH1903LV03toCH1903LV03Plus();
    private Zone2FacilitiesAssigner zone2FacilitiesAssigner;
    private int radius;

    public ZoneCoordinateReader(ActivityFacilities facilities, int radius) {
        this.zone2FacilitiesAssigner = new Zone2FacilitiesAssigner(facilities);
        this.radius = radius;
    }

    public Map<Integer, Set<ActivityFacility>> read(String zoneCoordFile) {
        Map<Integer, Set<ActivityFacility>> zones = new HashMap<>();

        // read zone centroids and assign all facilities close to centroid
        Counter counter = new Counter(" zone # ");
        BufferedReader reader = IOUtils.getBufferedReader(zoneCoordFile);
        try {
            String nextLine = reader.readLine();
            while (nextLine != null) {
                String[] line = nextLine.split(DELIMITER);
                int zoneId = Integer.parseInt(line[0]);
                if (zoneId < 2000000) { // these are the Swiss zones
                    double xCoord = Double.parseDouble(line[2]);
                    double yCoord = Double.parseDouble(line[3]);
                    Coord coord = transformation.transform(new Coord(xCoord, yCoord));
                    zones.put(zoneId, zone2FacilitiesAssigner.assign(coord, radius));
                }
                nextLine = reader.readLine();
                counter.incCounter();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        counter.printCounter();

        return zones;
    }
}
