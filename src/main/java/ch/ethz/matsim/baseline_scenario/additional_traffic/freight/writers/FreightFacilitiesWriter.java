package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.writers;

import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;

public class FreightFacilitiesWriter {
    private final ActivityFacilities freightFacilities;

    public FreightFacilitiesWriter(final ActivityFacilities freightFacilities) {
        this.freightFacilities = freightFacilities;
    }

    public void write(String outputPath) {
        FacilitiesWriter writer = new FacilitiesWriter(freightFacilities);
        writer.write(outputPath);
    }
}
