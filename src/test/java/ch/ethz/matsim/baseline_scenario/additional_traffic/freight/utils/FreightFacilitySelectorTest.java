package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.ZoneItem;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;

import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FreightFacilitySelectorTest {

    @Test
    public void getFreightFacility() {

        Map<Integer, ZoneItem> zone2facilities = new HashMap<>();

        int zoneId = 0;
        String name = "zone";
        Coord coord = new Coord(0.0, 0.0);
        zone2facilities.put(zoneId, new ZoneItem(zoneId, name, coord, new HashSet<>()));

        ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
        for (int i=0; i<100; i++) {
            Id<ActivityFacility> facilityId = Id.create("facility_" + Integer.toString(i), ActivityFacility.class);
            ActivityFacility facility = activityFacilitiesFactory.createActivityFacility(facilityId,
                    coord,
                    Id.createLinkId("link"));
            zone2facilities.get(zoneId).getFacilities().add(facility);
        }

        Random random = new Random(0);
        FreightFacilitySelector freightFacilitySelector = new FreightFacilitySelector(zone2facilities, random);
        for (int i=0; i<100000; i++) {
            Optional<ActivityFacility> facility = freightFacilitySelector.getFreightFacility(0);
            assertTrue(facility.isPresent());
        }

    }
}