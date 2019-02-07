package ch.ethz.matsim.baseline_scenario.paris;

import org.matsim.api.core.v01.Coord;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class IRIS {
	private final static GeometryFactory factory = new GeometryFactory();

	public final Geometry geometry;
	public final String code;

	public IRIS(Geometry geometry, String code) {
		this.geometry = geometry;
		this.code = code;
	}

	public boolean containsCoordinate(Coord coord) {
		Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
		Point point = factory.createPoint(coordinate);
		return geometry.contains(point);
	}
}
