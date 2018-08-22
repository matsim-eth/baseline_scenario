package ch.ethz.matsim.baseline_scenario.analysis.counts.utils.compatibility;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.DailyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.CSVDailyCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.readers.CSVHourlyCountsReader;
import ch.ethz.matsim.baseline_scenario.analysis.counts.writers.CSVDailyCountsWriter;
import ch.ethz.matsim.baseline_scenario.analysis.counts.writers.CSVHourlyCountsWriter;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class RunCountNetworkMapper {
    static public void main(String[] args) throws IOException {
        String mode = args[0];
        String deprecatedNetworkInputPath = args[1];
        String recentNetworkInputPath = args[2];
        String deprecatedCountsInputPath = args[3];
        String newCountsOutputPath = args[4];

        Network fullDeprecatedNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(fullDeprecatedNetwork).readFile(deprecatedNetworkInputPath);

        Network deprecatedNetwork = NetworkUtils.createNetwork();
        new TransportModeNetworkFilter(fullDeprecatedNetwork).filter(deprecatedNetwork, Collections.singleton("car"));

        Network fullCurrentNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(fullCurrentNetwork).readFile(recentNetworkInputPath);

        Network currentNetwork = NetworkUtils.createNetwork();
        new TransportModeNetworkFilter(fullCurrentNetwork).filter(currentNetwork, Collections.singleton("car"));

        if (mode.equals("daily")) {
            Collection<DailyCountItem> countItems = new CSVDailyCountsReader().read(deprecatedCountsInputPath);
            new CountNetworkMapper(deprecatedNetwork, currentNetwork).run(countItems);
            new CSVDailyCountsWriter(countItems).write(newCountsOutputPath);
        } else if (mode.equals("hourly")) {
            Collection<HourlyCountItem> countItems = new CSVHourlyCountsReader().read(deprecatedCountsInputPath);
            new CountNetworkMapper(deprecatedNetwork, currentNetwork).run(countItems);
            new CSVHourlyCountsWriter(countItems).write(newCountsOutputPath);
        } else {
            throw new IllegalArgumentException();
        }
    }
}
