package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers;

import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;

public class CumulativeDepartureProbabilityReader {
    private final static String DELIMITER = ";";

    public double[] read(String cumulativeProbabilityFreightDeparturesFile) {
        double[] cumulativeDepartureProbability = new double[24];
        BufferedReader reader = IOUtils.getBufferedReader(cumulativeProbabilityFreightDeparturesFile);
        try {
            for (int i = 0; i < 24; i++) {
                String[] line = reader.readLine().split(DELIMITER);
                cumulativeDepartureProbability[i] = Double.parseDouble(line[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cumulativeDepartureProbability;
    }
}
