package ch.matsim.baseline_scenario.zurich.cutter.plan.points;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.DefaultTransitRouteCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TransitRouteCrossingPoint;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TransitRouteCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class TestDefaultTransitRouteCrossingPointFinder {
	final private static TransitSchedule transitSchedule;
	final private static TransitLine transitLine;
	final private static TransitRoute transitRoute;

	static {
		/*
		 * Line departs @ t = [10, 20, 30, 40, 50, 60, 70], has stops @ x = [1.0 ...
		 * 10.0], needs 5s per stop
		 */

		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		List<TransitRouteStop> stops = new LinkedList<>();
		List<TransitStopFacility> facilities = new LinkedList<>();

		for (int i = 1; i <= 10; i++) {
			TransitStopFacility facility = factory.createTransitStopFacility(
					Id.create("fac" + i, TransitStopFacility.class), new Coord(i, 0.0), false);
			stops.add(factory.createTransitRouteStop(facility, (i - 1) * 5.0 - 1.0, (i - 1) * 5.0));
			facilities.add(facility);
		}

		transitRoute = factory.createTransitRoute(Id.create("R", TransitRoute.class), null, stops, "rail");
		transitLine = factory.createTransitLine(Id.create("R", TransitLine.class));
		transitSchedule = factory.createTransitSchedule();
		facilities.forEach(transitSchedule::addStopFacility);
		transitSchedule.addTransitLine(transitLine);
		transitLine.addRoute(transitRoute);
	}

	final private static ExperimentalTransitRoute routeMock;

	static {
		TransitStopFacility accessFacility = transitSchedule.getFacilities()
				.get(Id.create("fac4", TransitStopFacility.class));
		TransitStopFacility egressFacility = transitSchedule.getFacilities()
				.get(Id.create("fac7", TransitStopFacility.class));

		routeMock = new ExperimentalTransitRoute(accessFacility, transitLine, transitRoute, egressFacility);
	}

	final private static DepartureFinder departureFinderMock = new DepartureFinder() {
		@Override
		public Departure findDeparture(TransitRoute route, TransitRouteStop accessStop, double departureTime) {
			return new TransitScheduleFactoryImpl().createDeparture(Id.create("dep", Departure.class), 50.0);
		}
	};

	static ScenarioExtent createExtentMock(double... inside) {
		List<Double> _inside = new LinkedList<>();

		for (int i = 0; i < inside.length; i++) {
			_inside.add(inside[i]);
		}

		return new ScenarioExtent() {
			@Override
			public boolean isInside(Coord coord) {
				return _inside.contains(coord.getX());
			}

			@Override
			public List<Coord> computeCrowflyCrossings(Coord from, Coord to) {
				throw new IllegalStateException();
			}

			@Override
			public Coord getReferencePoint() {
				return null;
			}
		};
	}

	@Test
	public void testFindCrossingPoints() {
		ScenarioExtent extent;
		TransitRouteCrossingPointFinder finder;
		List<TransitRouteCrossingPoint> result;

		// 1) Outside -> Inside
		extent = createExtentMock(6.0, 7.0);
		finder = new DefaultTransitRouteCrossingPointFinder(extent, transitSchedule, departureFinderMock);
		result = finder.findCrossingPoints(routeMock, 25.0);

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(50.0 + 15.0 + 5.0, result.get(0).outsideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 10.0, result.get(0).insideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(4), result.get(0).outsideStop);
		Assert.assertEquals(transitRoute.getStops().get(5), result.get(0).insideStop);
		Assert.assertFalse(result.get(0).isOutgoing);

		// 2) Inside -> Outside
		extent = createExtentMock(4.0, 5.0);
		finder = new DefaultTransitRouteCrossingPointFinder(extent, transitSchedule, departureFinderMock);
		result = finder.findCrossingPoints(routeMock, 25.0);

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(50.0 + 15.0 + 5.0, result.get(0).insideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 10.0, result.get(0).outsideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(4), result.get(0).insideStop);
		Assert.assertEquals(transitRoute.getStops().get(5), result.get(0).outsideStop);
		Assert.assertTrue(result.get(0).isOutgoing);

		// 3) Inside -> Outside -> Inside
		extent = createExtentMock(4.0, 7.0);
		finder = new DefaultTransitRouteCrossingPointFinder(extent, transitSchedule, departureFinderMock);
		result = finder.findCrossingPoints(routeMock, 25.0);

		Assert.assertEquals(2, result.size());
		Assert.assertEquals(50.0 + 15.0, result.get(0).insideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 5.0, result.get(0).outsideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(3), result.get(0).insideStop);
		Assert.assertEquals(transitRoute.getStops().get(4), result.get(0).outsideStop);
		Assert.assertTrue(result.get(0).isOutgoing);
		Assert.assertEquals(50.0 + 15.0 + 10.0, result.get(1).outsideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 15.0, result.get(1).insideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(5), result.get(1).outsideStop);
		Assert.assertEquals(transitRoute.getStops().get(6), result.get(1).insideStop);
		Assert.assertFalse(result.get(1).isOutgoing);

		// 4) Outside -> Inside -> Outside
		extent = createExtentMock(5.0, 6.0);
		finder = new DefaultTransitRouteCrossingPointFinder(extent, transitSchedule, departureFinderMock);
		result = finder.findCrossingPoints(routeMock, 25.0);

		Assert.assertEquals(2, result.size());
		Assert.assertEquals(50.0 + 15.0, result.get(0).outsideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 5.0, result.get(0).insideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(3), result.get(0).outsideStop);
		Assert.assertEquals(transitRoute.getStops().get(4), result.get(0).insideStop);
		Assert.assertFalse(result.get(0).isOutgoing);
		Assert.assertEquals(50.0 + 15.0 + 10.0, result.get(1).insideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 15.0, result.get(1).outsideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(5), result.get(1).insideStop);
		Assert.assertEquals(transitRoute.getStops().get(6), result.get(1).outsideStop);
		Assert.assertTrue(result.get(1).isOutgoing);

		// 5) Inside -> Outside -> Inside -> Outside
		extent = createExtentMock(4.0, 6.0);
		finder = new DefaultTransitRouteCrossingPointFinder(extent, transitSchedule, departureFinderMock);
		result = finder.findCrossingPoints(routeMock, 25.0);
		Assert.assertEquals(3, result.size());
	}
}
