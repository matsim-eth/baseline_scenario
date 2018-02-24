package ch.matsim.baseline_scenario.zurich.cutter.plan.points;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.DefaultTeleportationCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TeleportationCrossingPoint;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TeleportationCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class TestDefaultTeleportationCrossingPointFinder {
	final private static ScenarioExtent extentMock = new ScenarioExtent() {
		@Override
		public boolean isInside(Coord coord) {
			return (coord.getX() > 2.0 && coord.getX() < 6.0) || (coord.getX() > 10.0 && coord.getX() < 14.0);
		}

		@Override
		public List<Coord> computeCrowflyCrossings(Coord from, Coord to) {
			return Arrays.asList(2.0, 6.0, 10.0, 14.0).stream().filter(x -> from.getX() < x && to.getX() > x)
					.map(x -> new Coord(x, 0.0)).collect(Collectors.toList());
		}

		@Override
		public Coord getReferencePoint() {
			return null;
		}
	};

	@Test
	public void testFindCrossingPoints() {
		TeleportationCrossingPointFinder finder = new DefaultTeleportationCrossingPointFinder(extentMock);

		List<TeleportationCrossingPoint> result;

		// 1) Outside -> Inside
		result = finder.findCrossingPoints(new Coord(0.0, 0.0), new Coord(4.0, 0.0), 400.0, 50.0);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(250.0, result.get(0).time, 1e-3);
		Assert.assertEquals(new Coord(2.0, 0.0), result.get(0).coord);
		Assert.assertFalse(result.get(0).isOutgoing);

		// 2) Inside -> Outside
		result = finder.findCrossingPoints(new Coord(4.0, 0.0), new Coord(8.0, 0.0), 400.0, 50.0);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(250.0, result.get(0).time, 1e-3);
		Assert.assertEquals(new Coord(6.0, 0.0), result.get(0).coord);
		Assert.assertTrue(result.get(0).isOutgoing);

		// 3) Inside -> Outside -> Inside
		result = finder.findCrossingPoints(new Coord(4.0, 0.0), new Coord(12.0, 0.0), 800.0, 50.0);
		Assert.assertEquals(2, result.size());
		Assert.assertEquals(250.0, result.get(0).time, 1e-3);
		Assert.assertEquals(new Coord(6.0, 0.0), result.get(0).coord);
		Assert.assertTrue(result.get(0).isOutgoing);
		Assert.assertEquals(650.0, result.get(1).time, 1e-3);
		Assert.assertEquals(new Coord(10.0, 0.0), result.get(1).coord);
		Assert.assertFalse(result.get(1).isOutgoing);

		// 4) Outside -> Inside -> Outside
		result = finder.findCrossingPoints(new Coord(0.0, 0.0), new Coord(8.0, 0.0), 800.0, 50.0);
		Assert.assertEquals(2, result.size());
		Assert.assertEquals(250.0, result.get(0).time, 1e-3);
		Assert.assertEquals(new Coord(2.0, 0.0), result.get(0).coord);
		Assert.assertFalse(result.get(0).isOutgoing);
		Assert.assertEquals(650.0, result.get(1).time, 1e-3);
		Assert.assertEquals(new Coord(6.0, 0.0), result.get(1).coord);
		Assert.assertTrue(result.get(1).isOutgoing);

		// 4) Outside -> Inside -> Outside -> Inside -> Outside
		result = finder.findCrossingPoints(new Coord(0.0, 0.0), new Coord(16.0, 0.0), 800.0, 50.0);
		Assert.assertEquals(4, result.size());
	}
}
