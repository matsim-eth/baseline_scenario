package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.FreightTrafficODItem;
import org.apache.log4j.Logger;
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
                String originName = row.get(header.indexOf("from_location"));
                int destinationZone = Integer.parseInt(row.get(header.indexOf("to_zone_id")));
                String destinationName = row.get(header.indexOf("to_location"));
                double numberOfTrips = Double.parseDouble(row.get(header.indexOf("number_trips")));

                freightTrafficODItems.add(new FreightTrafficODItem(originZone, originName, destinationZone, destinationName, numberOfTrips, type));
                counter.incCounter();
            }
        }

        reader.close();
        counter.printCounter();
        return freightTrafficODItems;
    }
}
