package ch.ethz.matsim.baseline_scenario.zurich.cutter.connector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class OutsideFacilityAdapter {
	final private ActivityFacilities facilities;

	final private Map<Coord, ActivityFacility> facilitiesByCoordinate = new HashMap<>();
	final private Map<Link, ActivityFacility> facilitiesByLink = new HashMap<>();

	private int counter = 0;

	public OutsideFacilityAdapter(ActivityFacilities facilities) {
		this.facilities = facilities;
	}

	private Id<ActivityFacility> createNextId() {
		counter++;
		return Id.create("outside_" + counter, ActivityFacility.class);
	}

	public ActivityFacility getFacility(Coord coord) {
		ActivityFacility facility = facilitiesByCoordinate.get(coord);

		if (facility == null) {
			Id<ActivityFacility> facilityId = createNextId();
			facility = facilities.getFactory().createActivityFacility(facilityId, coord,
					Id.createLinkId(facilityId.toString()));
			facilitiesByCoordinate.put(coord, facility);
			facilities.addActivityFacility(facility);
		}

		return facility;
	}

	public ActivityFacility getFacility(Link link) {
		ActivityFacility facility = facilitiesByLink.get(link);

		if (facility == null) {
			facility = facilities.getFactory().createActivityFacility(createNextId(), link.getCoord(), link.getId());
			facilitiesByLink.put(link, facility);
			facilities.addActivityFacility(facility);
		}

		return facility;
	}

	public Collection<ActivityFacility> getDetachedFacilities() {
		return facilitiesByCoordinate.values();
	}
}
