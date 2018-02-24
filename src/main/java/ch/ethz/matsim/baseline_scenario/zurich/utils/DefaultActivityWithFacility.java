package ch.ethz.matsim.baseline_scenario.zurich.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class DefaultActivityWithFacility implements ActivityWithFacility {
	final private Activity delegate;
	final private ActivityFacility facility;

	public DefaultActivityWithFacility(Activity delegate, ActivityFacility facility) {
		this.delegate = delegate;
		this.facility = facility;
	}

	@Override
	public ActivityFacility getFacility() {
		return facility;
	}

	@Override
	public double getEndTime() {
		return delegate.getEndTime();
	}

	@Override
	public void setEndTime(double seconds) {
		delegate.setEndTime(seconds);
	}

	@Override
	public String getType() {
		return delegate.getType();
	}

	@Override
	public void setType(String type) {
		delegate.setType(type);
	}

	@Override
	public Coord getCoord() {
		return delegate.getCoord();
	}

	@Override
	public double getStartTime() {
		return delegate.getStartTime();
	}

	@Override
	public void setStartTime(double seconds) {
		delegate.setStartTime(seconds);
	}

	@Override
	public double getMaximumDuration() {
		return delegate.getMaximumDuration();
	}

	@Override
	public void setMaximumDuration(double seconds) {
		delegate.setMaximumDuration(seconds);
	}

	@Override
	public Id<Link> getLinkId() {
		return delegate.getLinkId();
	}

	@Override
	public Id<ActivityFacility> getFacilityId() {
		return delegate.getFacilityId();
	}

	@Override
	public void setLinkId(Id<Link> id) {
		delegate.setLinkId(id);
	}

	@Override
	public void setFacilityId(Id<ActivityFacility> id) {
		delegate.setFacilityId(id);
	}

	@Override
	public void setCoord(Coord coord) {
		delegate.setCoord(coord);
	}

	@Override
	public Attributes getAttributes() {
		return delegate.getAttributes();
	}
}
