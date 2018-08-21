package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.run;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.FreightTrafficCreator;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.FreightTrafficODItem;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers.CumulativeDepartureProbabilityReader;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers.FreightTrafficODReader;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.readers.ZoneReader;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils.DepartureTimeGenerator;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils.FreightFacilitySelector;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;

import java.io.File;
import java.util.*;

public class RunFreightTrafficCreator {
    public static void main(String[] args) {
        final String configFile = args[0];
        final String zoneCoordinatesFile = args[1];
        final String utilityVehiclesFile = args[2];
        final String trucksFile = args[3];
        final String tractorTrailersFile = args[4];
        final String cumulativeProbabilityFreightDeparturesFile = args[5];
        final double scalingFactor = Double.parseDouble(args[6]); // for example for a 1% population enter "0.01"
        final int randomSeed = Integer.parseInt(args[7]);
        final String outputPath = args[8];

        Random random = new Random(randomSeed);

        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));

        DepartureTimeGenerator departureTimeGenerator = new DepartureTimeGenerator(random,
                (new CumulativeDepartureProbabilityReader().read(cumulativeProbabilityFreightDeparturesFile)));

        Map<Integer, Set<ActivityFacility>> zone2facilities = new ZoneReader(scenario.getActivityFacilities(), 1000)
                .read(zoneCoordinatesFile);

        FreightFacilitySelector freightFacilitySelector = new FreightFacilitySelector(zone2facilities, random);

        FreightTrafficODReader freightTrafficODReader = new FreightTrafficODReader();
        List<FreightTrafficODItem> freightTrafficODItems = new LinkedList<>();
        freightTrafficODItems.addAll(freightTrafficODReader.read("UtilityVehicle", utilityVehiclesFile));
        freightTrafficODItems.addAll(freightTrafficODReader.read("Truck", trucksFile));
        freightTrafficODItems.addAll(freightTrafficODReader.read("TractorTrailer", tractorTrailersFile));

        new FreightTrafficCreator(random, scalingFactor,
                freightTrafficODItems, freightFacilitySelector, departureTimeGenerator)
                .run(scenario.getPopulation(), scenario.getActivityFacilities());

        new PopulationWriter(scenario.getPopulation())
                .write(new File(outputPath, "test_population.xml.gz").getPath());
        new FacilitiesWriter(scenario.getActivityFacilities())
                .write(new File(outputPath, "test_facilities.xml.gz").getPath());
    }
}
