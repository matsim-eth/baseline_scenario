package ch.ethz.matsim.baseline_scenario.zurich.cutter.schedule;

import java.util.LinkedList;
import java.util.List;

import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

/*
 * TODO: This class should be used in TransitRouteCrossingPointFinder
 */
public class DefaultStopSequenceCrossingPointFinder implements StopSequenceCrossingPointFinder {
	final private ScenarioExtent extent;

	public DefaultStopSequenceCrossingPointFinder(ScenarioExtent extent) {
		this.extent = extent;
	}

	@Override
	public List<StopSequenceCrossingPoint> findCrossingPoints(List<TransitRouteStop> stopSequence) {
		List<StopSequenceCrossingPoint> crossingPoints = new LinkedList<>();

		for (int i = 0; i < stopSequence.size() - 1; i++) {
			TransitRouteStop firstStop = stopSequence.get(i);
			TransitRouteStop secondStop = stopSequence.get(i + 1);

			boolean firstIsInside = extent.isInside(firstStop.getStopFacility().getCoord());
			boolean secondIsInside = extent.isInside(secondStop.getStopFacility().getCoord());

			if (firstIsInside != secondIsInside) { // We found a crossing
				TransitRouteStop insideStop = firstIsInside ? firstStop : secondStop;
				TransitRouteStop outsideStop = firstIsInside ? secondStop : firstStop;

				crossingPoints.add(new StopSequenceCrossingPoint(insideStop, outsideStop, firstIsInside, i));
			}
		}

		return crossingPoints;
	}
}
