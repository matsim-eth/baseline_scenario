package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items;

import org.matsim.api.core.v01.Coord;
import org.matsim.facilities.ActivityFacility;

import java.util.Set;

public class ZoneItem {
    private int id;
    private String name;
    private Coord coord;
    private Set<ActivityFacility> facilities;

    public ZoneItem(int id, String name, Coord coord, Set<ActivityFacility> facilities) {
        this.id = id;
        this.name = name;
        this.coord = coord;
        this.facilities = facilities;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Coord getCoord() {
        return coord;
    }

    public Set<ActivityFacility> getFacilities() {
        return facilities;
    }
}
