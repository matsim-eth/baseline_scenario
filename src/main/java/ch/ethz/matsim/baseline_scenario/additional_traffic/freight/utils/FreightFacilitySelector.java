package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils;

import ch.ethz.matsim.baseline_scenario.additional_traffic.AdditionalTrafficType;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.ZoneItem;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.*;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class FreightFacilitySelector {
    private final Map<Integer, ZoneItem> zoneItems;
    private final ActivityFacilitiesFactory activityFacilitiesFactory = new ActivityFacilitiesFactoryImpl();
    private final Random random;

    public FreightFacilitySelector(Map<Integer, ZoneItem> zoneItems, Random random) {
        this.zoneItems = zoneItems;
        this.random = random;
    }

    public ActivityFacility getFreightFacility(int zoneId) {
        Set<ActivityFacility> facilityList = zoneItems.get(zoneId).getFacilities();
        Optional<ActivityFacility> facility = Optional.empty();

        System.out.println("Zone : " + Integer.toString(zoneId) + " , # facilities : " + facilityList.size());

        while (!facility.isPresent()) {
            facility = facilityList.stream()
                    .skip(random.nextInt(facilityList.size()))
                    .findFirst();
        }
        Id<ActivityFacility> facilityId = Id.create(AdditionalTrafficType.FREIGHT.toString()
                + "_" + (int)facility.get().getCoord().getX() + "_" + (int)facility.get().getCoord().getY(), ActivityFacility.class);
        ActivityFacility freightFacility = activityFacilitiesFactory.createActivityFacility(facilityId,
                facility.get().getCoord(), facility.get().getLinkId());

        // add freight activity to facility
        freightFacility.addActivityOption(new ActivityOptionImpl(AdditionalTrafficType.FREIGHT.toString()));
        OpeningTime openingTime = new OpeningTimeImpl(0.0 * 3600.0, 24.0 * 3600.0);
        freightFacility.getActivityOptions().get(AdditionalTrafficType.FREIGHT.toString()).addOpeningTime(openingTime);

        return freightFacility;
    }
}
