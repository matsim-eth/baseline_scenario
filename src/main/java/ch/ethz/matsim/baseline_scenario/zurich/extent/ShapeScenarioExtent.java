package ch.ethz.matsim.baseline_scenario.zurich.extent;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Coord;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ShapeScenarioExtent implements ScenarioExtent {
	private final GeometryFactory factory = new GeometryFactory();
	private final Polygon polygon;

	public ShapeScenarioExtent(Polygon polygon) {
		this.polygon = polygon;
	}

	@Override
	public boolean isInside(Coord coord) {
		Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
		Point point = factory.createPoint(coordinate);
		return polygon.contains(point);
	}

	@Override
	public List<Coord> computeCrowflyCrossings(Coord from, Coord to) {
		if (from.equals(to)) {
			return Collections.emptyList();
		}

		Coordinate fromCoordinate = new Coordinate(from.getX(), to.getY());
		Coordinate toCoordinate = new Coordinate(from.getX(), to.getY());

		LineString line = factory.createLineString(new Coordinate[] { fromCoordinate, toCoordinate });
		Coordinate[] crossingCoordinates = ((LineString) polygon.intersection(line)).getCoordinates();

		List<Coord> crossings = new ArrayList<>(crossingCoordinates.length);

		for (int i = 0; i < crossingCoordinates.length; i++) {
			Coordinate crossingCoordinate = crossingCoordinates[i];
			crossings.add(new Coord(crossingCoordinate.x, crossingCoordinate.y));
		}

		return crossings;
	}

	@Override
	public Coord getReferencePoint() {
		Coordinate coordinate = polygon.getInteriorPoint().getCoordinate();
		return new Coord(coordinate.x, coordinate.y);
	}

	static public class Builder {
		private final File path;

		public Builder(File path) {
			this.path = path;
		}

		public ShapeScenarioExtent build() throws MalformedURLException, IOException {
			DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", path.toURI().toURL()));
			SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
			SimpleFeatureCollection featureCollection = featureSource.getFeatures();
			SimpleFeatureIterator featureIterator = featureCollection.features();

			if (featureCollection.size() != 1) {
				throw new IllegalStateException("Expecting exactly one feature in extent shape file.");
			}

			SimpleFeature feature = featureIterator.next();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			Polygon polygon = null;

			if (geometry instanceof MultiPolygon) {
				MultiPolygon multiPolygon = (MultiPolygon) geometry;

				if (multiPolygon.getNumGeometries() != 1) {
					throw new IllegalStateException("Extent shape is non-connected.");
				}

				polygon = (Polygon) multiPolygon.getGeometryN(0);
			} else if (geometry instanceof Polygon) {
				polygon = (Polygon) geometry;
			} else {
				throw new IllegalStateException("Expecting polygon geometry!");
			}

			featureIterator.close();
			dataStore.dispose();

			return new ShapeScenarioExtent(polygon);
		}
	}
}
