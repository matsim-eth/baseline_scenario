package ch.ethz.matsim.baseline_scenario.additional_traffic.freight;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.FreightTrafficODItem;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers.CumulativeDepartureProbabilityReader;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers.FreightTrafficODReader;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers.ZoneReader;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils.FreightFacilitySelector;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.writers.FreightFacilitiesWriter;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.writers.FreightPopulationWriter;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils.DepartureTimeGenerator;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.*;

public class RunFreightTrafficCreator {
    public static void main(String[] args) {
        final String zoneCoordinatesFile = args[0];
        final String scenarioFacilitiesFile = args[1]; // all scenario facilities incl secondary facilities and bc facilities.
        final String utilityVehiclesFile = args[2];
        final String trucksFile = args[3];
        final String tractorTrailersFile = args[4];
        final String cumulativeProbabilityFreightDeparturesFile = args[5];
        final double scalingFactor = Double.parseDouble(args[6]); // for example for a 1% population enter "0.01"
        final int randomSeed = Integer.parseInt(args[7]);
        final String outputFacilities = args[8];
        final String outputPopulation = args[9];

        Random random = new Random(randomSeed);

        DepartureTimeGenerator departureTimeGenerator = new DepartureTimeGenerator(random,
                (new CumulativeDepartureProbabilityReader().read(cumulativeProbabilityFreightDeparturesFile)));


        // TODO : HOW DO I SIMPLY LOAD ACTIVITY FACILITIES?
        ActivityFacilities facilities = null;
        Map<Integer, Set<ActivityFacility>> zone2facilities = new ZoneReader(facilities, 1000).read(zoneCoordinatesFile);


        FreightFacilitySelector freightFacilitySelector = new FreightFacilitySelector(zone2facilities, random);

        FreightTrafficODReader freightTrafficODReader = new FreightTrafficODReader();
        List<FreightTrafficODItem> freightTrafficODItems = new LinkedList<>();
        freightTrafficODItems.addAll(freightTrafficODReader.read("UtilityVehicle", utilityVehiclesFile));
        freightTrafficODItems.addAll(freightTrafficODReader.read("Truck", trucksFile));
        freightTrafficODItems.addAll(freightTrafficODReader.read("TractorTrailer", tractorTrailersFile));


        FreightTraffic freightTraffic = new FreightTrafficCreator(random, scalingFactor,
                freightTrafficODItems, freightFacilitySelector, departureTimeGenerator).create();

        new FreightFacilitiesWriter(freightTraffic.getFacilities()).write(outputFacilities);
        new FreightPopulationWriter(freightTraffic.getPopulation()).write(outputPopulation);
    }
}
