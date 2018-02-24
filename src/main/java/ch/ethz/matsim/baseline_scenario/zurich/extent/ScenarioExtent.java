package ch.ethz.matsim.baseline_scenario.zurich.extent;

import java.util.List;

import org.matsim.api.core.v01.Coord;

public interface ScenarioExtent {
	boolean isInside(Coord coord);

	List<Coord> computeCrowflyCrossings(Coord from, Coord to);
	
	Coord getReferencePoint();
}
