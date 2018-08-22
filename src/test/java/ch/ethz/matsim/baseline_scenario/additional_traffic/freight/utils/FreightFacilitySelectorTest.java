package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils;

import ch.ethz.matsim.baseline_scenario.additional_traffic.AdditionalTrafficType;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;

import java.util.*;

import static org.junit.Assert.*;

public class FreightFacilitySelectorTest {

    @Test
    public void getFreightFacility() {

        Map<Integer, Set<ActivityFacility>> zone2facilities = new HashMap<>();

        zone2facilities.putIfAbsent(0, new HashSet<>());
        ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
        for (int i=0; i<100; i++) {
            Id<ActivityFacility> facilityId = Id.create("facility_" + Integer.toString(i), ActivityFacility.class);
            ActivityFacility facility = activityFacilitiesFactory.createActivityFacility(facilityId,
                    new Coord(0.0, 0.0),
                    Id.createLinkId("link"));
            zone2facilities.get(0).add(facility);
        }

        Random random = new Random(0);
        FreightFacilitySelector freightFacilitySelector = new FreightFacilitySelector(zone2facilities, random);
        for (int i=0; i<100000; i++) {
            ActivityFacility facility = freightFacilitySelector.getFreightFacility(0);
            assertNotNull(facility);
        }

    }
}