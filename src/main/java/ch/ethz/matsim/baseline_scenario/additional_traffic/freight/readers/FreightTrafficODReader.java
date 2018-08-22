package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.FreightTrafficODItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class FreightTrafficODReader {
    private static final Logger log = Logger.getLogger(FreightTrafficODReader.class);
    private final static String DELIMITER = ";";

    public Collection<FreightTrafficODItem> read(String type, String path) throws IOException{
        log.info("Trying to load " + path);
        List<FreightTrafficODItem> freightTrafficODItems = new LinkedList<>();

        Counter counter = new Counter(" OD-relationship # ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(DELIMITER));

            if (header == null) {
                header = row;
            } else {
                int originZone = Integer.parseInt(row.get(header.indexOf("from_zone_id")));
                int originName = row.get(header.indexOf("from_zone_id"));
                int destinationZone = Integer.parseInt(line[2]);
                double numberOfTrips = Double.parseDouble(line[4]);



                String countStationId = row.get(header.indexOf("countStationId"));
                Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("link")));
                int referenceCount = Integer.parseInt(row.get(header.indexOf("reference_count")));
                int simulationCount = Integer.parseInt(row.get(header.indexOf("simulation_count")));
                double x = Double.parseDouble(row.get(header.indexOf("location_x")));
                double y = Double.parseDouble(row.get(header.indexOf("location_y")));
                Coord location = new Coord(x, y);

                DailyCountItem item = new DailyCountItem(linkId, referenceCount, location, countStationId);
                item.simulation = simulationCount;

                items.add(item);
            }
        }

        reader.close();
        return items;




        try {
            String nextLine = reader.readLine();
            while (nextLine != null) {
                String[] line = nextLine.split(DELIMITER);

                int originZone = Integer.parseInt(line[0]);
                int destinationZone = Integer.parseInt(line[2]);
                double numberOfTrips = Double.parseDouble(line[4]);

                freightTrafficODItems.add(new FreightTrafficODItem(originZone, destinationZone, numberOfTrips, type));
                nextLine = reader.readLine();
                counter.incCounter();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        counter.printCounter();

        return freightTrafficODItems;
    }
}
