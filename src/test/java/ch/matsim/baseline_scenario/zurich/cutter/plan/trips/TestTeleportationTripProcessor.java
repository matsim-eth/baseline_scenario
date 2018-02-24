package ch.matsim.baseline_scenario.zurich.cutter.plan.trips;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TeleportationCrossingPoint;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TeleportationCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.TeleportationTripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class TestTeleportationTripProcessor {
	static private class TeleportationFinderMock implements TeleportationCrossingPointFinder {
		final private List<TeleportationCrossingPoint> points = new LinkedList<>();

		public void add(TeleportationCrossingPoint point) {
			points.add(point);
		}

		@Override
		public List<TeleportationCrossingPoint> findCrossingPoints(Coord originCoord, Coord destinationCoord,
				double originalTravelTime, double departureTime) {
			return points;
		}
	}

	static ScenarioExtent scenarioExtentMock = new ScenarioExtent() {
		@Override
		public boolean isInside(Coord coord) {
			return false;
		}

		@Override
		public List<Coord> computeCrowflyCrossings(Coord from, Coord to) {
			return null;
		}

		@Override
		public Coord getReferencePoint() {
			return null;
		}
	};

	@Test
	public void testTeleportationTripProcessor() {
		TeleportationFinderMock finderMock;
		TeleportationTripProcessor processor;
		List<PlanElement> result;

		// No crossing points
		finderMock = new TeleportationFinderMock();
		processor = new TeleportationTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, null, 0.0, 0.0, "walk", false);

		Assert.assertEquals(1, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertEquals("walk", ((Leg) result.get(0)).getMode());

		// One crossing point, outgoing
		finderMock = new TeleportationFinderMock();
		finderMock.add(new TeleportationCrossingPoint(new Coord(1.0, 0.0), 10.0, true));

		processor = new TeleportationTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, null, 0.0, 0.0, "walk", false);

		Assert.assertEquals(3, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertEquals("walk", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("outside", ((Leg) result.get(2)).getMode());
		Assert.assertEquals(10.0, ((Activity) result.get(1)).getEndTime(), 1e-3);
		Assert.assertEquals(new Coord(1.0, 0.0), ((Activity) result.get(1)).getCoord());

		// One crossing point, incoming
		finderMock = new TeleportationFinderMock();
		finderMock.add(new TeleportationCrossingPoint(new Coord(1.0, 0.0), 10.0, false));

		processor = new TeleportationTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, null, 0.0, 0.0, "walk", false);

		Assert.assertEquals(3, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertEquals("outside", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("walk", ((Leg) result.get(2)).getMode());
		Assert.assertEquals(10.0, ((Activity) result.get(1)).getEndTime(), 1e-3);
		Assert.assertEquals(new Coord(1.0, 0.0), ((Activity) result.get(1)).getCoord());

		// Two crossing points, inside -> outside -> inside
		finderMock = new TeleportationFinderMock();
		finderMock.add(new TeleportationCrossingPoint(new Coord(1.0, 0.0), 10.0, true));
		finderMock.add(new TeleportationCrossingPoint(new Coord(2.0, 0.0), 20.0, false));

		processor = new TeleportationTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, null, 0.0, 0.0, "walk", false);

		Assert.assertEquals(5, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertTrue(result.get(3) instanceof Activity);
		Assert.assertTrue(result.get(4) instanceof Leg);
		Assert.assertEquals("walk", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("outside", ((Leg) result.get(2)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(3)).getType());
		Assert.assertEquals("walk", ((Leg) result.get(4)).getMode());
		Assert.assertEquals(10.0, ((Activity) result.get(1)).getEndTime(), 1e-3);
		Assert.assertEquals(20.0, ((Activity) result.get(3)).getEndTime(), 1e-3);
		Assert.assertEquals(new Coord(1.0, 0.0), ((Activity) result.get(1)).getCoord());
		Assert.assertEquals(new Coord(2.0, 0.0), ((Activity) result.get(3)).getCoord());
	}
}
