package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.ZoneItem;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class FacilityToZoneAssigner {
    private static final Logger log = Logger.getLogger(FacilityToZoneAssigner.class);
    final private QuadTree<ZoneItem> quadtree;
    private Map<Integer, ZoneItem> zoneItems;

    public FacilityToZoneAssigner(Map<Integer, ZoneItem> zoneItems) {
        log.info("adding zone items to quadtree...");
        this.zoneItems = zoneItems;

        Collection<Coord> coords = new HashSet<>();
        zoneItems.values().forEach(z -> coords.add(z.getCoord()));

        double[] dimensions = getBoundingBox(coords);
        quadtree = new QuadTree<>(dimensions[0], dimensions[1], dimensions[2], dimensions[3]);
        zoneItems.values().forEach(z -> quadtree.put(z.getCoord().getX(), z.getCoord().getY(), z));

    }

    public void assign(ActivityFacilities facilities) {
        log.info("trying to assign facilities to nearest zone...");
        Counter counter = new Counter(" facility # ");
        for (ActivityFacility facility : facilities.getFacilities().values()) {
            if (facility.getActivityOptions().keySet().contains("work") ) {
                ZoneItem zoneItem = quadtree.getClosest(facility.getCoord().getX(), facility.getCoord().getY());
                zoneItems.get(zoneItem.getId()).getFacilities().add(facility);
                counter.incCounter();
            }
        }
        counter.printCounter();
    }

    private double[] getBoundingBox(final Collection<Coord> coords) {
        double[] bBox = new double[4];
        bBox[0] = Double.POSITIVE_INFINITY;
        bBox[1] = Double.POSITIVE_INFINITY;
        bBox[2] = Double.NEGATIVE_INFINITY;
        bBox[3] = Double.NEGATIVE_INFINITY;

        for (Coord c : coords) {
            if (c.getX() < bBox[0]) {
                bBox[0] = c.getX();
            }
            if (c.getX() > bBox[2]) {
                bBox[2] = c.getX();
            }
            if (c.getY() > bBox[3]) {
                bBox[3] = c.getY();
            }
            if (c.getY() < bBox[1]) {
                bBox[1] = c.getY();
            }
        }

        bBox[0] -= 1;
        bBox[1] -= 1;
        bBox[2] += 1;
        bBox[3] += 1;

        return bBox;
    }

}
