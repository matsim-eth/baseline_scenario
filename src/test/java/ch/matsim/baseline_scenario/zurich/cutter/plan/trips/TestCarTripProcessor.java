package ch.matsim.baseline_scenario.zurich.cutter.plan.trips;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.utils.objectattributes.attributable.Attributes;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.NetworkCrossingPoint;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.NetworkCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.CarTripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class TestCarTripProcessor {
	static private class NetworkFinderMock implements NetworkCrossingPointFinder {
		final private List<NetworkCrossingPoint> points = new LinkedList<>();

		@Override
		public List<NetworkCrossingPoint> findCrossingPoints(NetworkRoute route, double departureTime) {
			return points;
		}

		public void add(NetworkCrossingPoint point) {
			points.add(point);
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
	public void testCarTripProcessor() {
		NetworkFinderMock finderMock;
		CarTripProcessor processor;
		List<PlanElement> result;

		Link linkA = createLinkMock("A");
		Link linkB = createLinkMock("B");

		// No crossing points
		finderMock = new NetworkFinderMock();
		processor = new CarTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, 100.0, false);

		Assert.assertEquals(1, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertEquals("car", ((Leg) result.get(0)).getMode());

		// One crossing point, outgoing
		finderMock = new NetworkFinderMock();
		finderMock.add(new NetworkCrossingPoint(0, linkA, 10.0, 20.0, true));

		processor = new CarTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, 100.0, false);

		Assert.assertEquals(3, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertEquals("car", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("outside", ((Leg) result.get(2)).getMode());
		Assert.assertEquals(20.0, ((Activity) result.get(1)).getEndTime(), 1e-3);
		Assert.assertEquals(Id.createLinkId("A"), ((Activity) result.get(1)).getLinkId());

		// One crossing point, incoming
		finderMock = new NetworkFinderMock();
		finderMock.add(new NetworkCrossingPoint(0, linkA, 10.0, 20.0, false));

		processor = new CarTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, 100.0, false);

		Assert.assertEquals(3, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertEquals("outside", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("car", ((Leg) result.get(2)).getMode());
		Assert.assertEquals(20.0, ((Activity) result.get(1)).getEndTime(), 1e-3);
		Assert.assertEquals(Id.createLinkId("A"), ((Activity) result.get(1)).getLinkId());

		// Two crossing points, inside -> outside -> inside
		finderMock = new NetworkFinderMock();
		finderMock.add(new NetworkCrossingPoint(0, linkA, 10.0, 20.0, true));
		finderMock.add(new NetworkCrossingPoint(0, linkB, 30.0, 40.0, false));

		processor = new CarTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, 100.0, false);

		Assert.assertEquals(5, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertTrue(result.get(3) instanceof Activity);
		Assert.assertTrue(result.get(4) instanceof Leg);
		Assert.assertEquals("car", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("outside", ((Leg) result.get(2)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(3)).getType());
		Assert.assertEquals("car", ((Leg) result.get(4)).getMode());
		Assert.assertEquals(20.0, ((Activity) result.get(1)).getEndTime(), 1e-3);
		Assert.assertEquals(40.0, ((Activity) result.get(3)).getEndTime(), 1e-3);
		Assert.assertEquals(Id.createLinkId("A"), ((Activity) result.get(1)).getLinkId());
		Assert.assertEquals(Id.createLinkId("B"), ((Activity) result.get(3)).getLinkId());
	}

	static private Link createLinkMock(String id) {
		return new Link() {
			@Override
			public Coord getCoord() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Id<Link> getId() {
				return Id.createLinkId(id);
			}

			@Override
			public Attributes getAttributes() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean setFromNode(Node node) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean setToNode(Node node) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public Node getToNode() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Node getFromNode() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public double getLength() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public double getNumberOfLanes() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public double getNumberOfLanes(double time) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public double getFreespeed() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public double getFreespeed(double time) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public double getCapacity() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public double getCapacity(double time) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public void setFreespeed(double freespeed) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setLength(double length) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setNumberOfLanes(double lanes) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setCapacity(double capacity) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setAllowedModes(Set<String> modes) {
				// TODO Auto-generated method stub

			}

			@Override
			public Set<String> getAllowedModes() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public double getFlowCapacityPerSec() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public double getFlowCapacityPerSec(double time) {
				// TODO Auto-generated method stub
				return 0;
			}
		};
	}
}
