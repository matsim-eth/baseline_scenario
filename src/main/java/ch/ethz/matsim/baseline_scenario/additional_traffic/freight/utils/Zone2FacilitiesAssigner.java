package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.*;

public class Zone2FacilitiesAssigner {
    private final ActivityFacilities facilities;

    public Zone2FacilitiesAssigner(ActivityFacilities facilities) {
        this.facilities = facilities;
    }

    public Set<ActivityFacility> assign(Coord coord, final int radius) {
        Set<ActivityFacility> facilitiesWithinRadius = new HashSet<>();

        for (ActivityFacility facility : facilities.getFacilities().values()) {
            if (facility.getActivityOptions().keySet().contains("work")
                    && CoordUtils.calcEuclideanDistance(facility.getCoord(), coord) <= radius) {
                facilitiesWithinRadius.add(facility);
            }
        }

        if (facilitiesWithinRadius.isEmpty()) {
            Id<ActivityFacility> facilityId = Id.create("temp_" + coord.toString(), ActivityFacility.class);
            ActivityFacility newFacility = facilities.getFactory().createActivityFacility(facilityId, coord);
            facilitiesWithinRadius.add(newFacility);
        }

        return facilitiesWithinRadius;
    }

}
