package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.FreightTrafficODItem;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class FreightTrafficODReader {
    private final static String DELIMITER = ";";

    public Collection<FreightTrafficODItem> read(String freightType, String ODfile) {
        List<FreightTrafficODItem> freightTrafficODItems = new LinkedList<>();

        Counter counter = new Counter(" OD-relationship # ");
        BufferedReader reader = IOUtils.getBufferedReader(ODfile);
        try {
            String nextLine = reader.readLine();
            while (nextLine != null) {
                String[] line = nextLine.split(DELIMITER);

                int originZone = Integer.parseInt(line[0]);
                int destinationZone = Integer.parseInt(line[2]);
                double numberOfTrips = Double.parseDouble(line[4]);

                freightTrafficODItems.add(new FreightTrafficODItem(originZone, destinationZone, numberOfTrips, freightType));
                nextLine = reader.readLine();
                counter.incCounter();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        counter.printCounter();

        return null;
    }
}
