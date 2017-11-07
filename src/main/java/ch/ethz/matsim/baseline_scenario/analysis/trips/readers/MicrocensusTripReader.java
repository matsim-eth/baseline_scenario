package ch.ethz.matsim.baseline_scenario.analysis.trips.readers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import ch.ethz.matsim.baseline_scenario.analysis.trips.TripItem;

public class MicrocensusTripReader {
	public Collection<TripItem> readTrips(String microcensusInput) throws NumberFormatException, IOException {
		List<TripItem> tripItems = new LinkedList<>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(microcensusInput)));
		String line = null;

		List<String> header = null;
		List<String> row = null;
		
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:21781", "EPSG:2056");

		while ((line = reader.readLine()) != null) {
			row = Arrays.asList(line.trim().split("\t"));

			if (header == null) {
				header = row;
			} else {
				Id<Person> personId = Id.createPersonId(row.get(header.indexOf("HHNR")) + "_" + row.get(header.indexOf("ZIELPNR")));
				int personTripIndex = Integer.parseInt(row.get(header.indexOf("WEGNR")));

				Coord origin = new Coord(Double.parseDouble(row.get(header.indexOf("S_X_CH1903"))),
						Double.parseDouble(row.get(header.indexOf("S_Y_CH1903"))));
				Coord destination = new Coord(Double.parseDouble(row.get(header.indexOf("Z_X_CH1903"))),
						Double.parseDouble(row.get(header.indexOf("Z_Y_CH1903"))));

				double startTime = Double.parseDouble(row.get(header.indexOf("f51100"))) * 60;
				double arrivalTime = Double.parseDouble(row.get(header.indexOf("f51400"))) * 60;
				double travelTime = arrivalTime - startTime;

				double networkDistance = Double.parseDouble(row.get(header.indexOf("w_rdist")));

				String mode = getMode(Integer.parseInt(row.get(header.indexOf("wmittel"))));
				String purpose = getPurpose(Integer.parseInt(row.get(header.indexOf("wzweck1"))));

				int tripType = Integer.parseInt(row.get(header.indexOf("wzweck2")));
				boolean returning = tripType == 2;
				
				//int module = Integer.parseInt(row.get(header.indexOf("DMOD")));

				// TODO: Revise the modes and purposes again, check how Kirill did the mapping
				Coord originCoord = transformation.transform(origin);
				Coord destinationCoord = transformation.transform(destination);
				
				if (mode != null && purpose != null && tripType != 3) {
					tripItems.add(new TripItem(personId, personTripIndex, originCoord, destinationCoord, startTime, travelTime,
							networkDistance, mode, purpose, returning, CoordUtils.calcEuclideanDistance(originCoord, destinationCoord) / 1000.0));
				}
			}
		}

		return tripItems;
	}

	private String getPurpose(int purposeIndex) {
		switch (purposeIndex) {
		case 2:
			return "work";
		case 3:
			return "education";
		case 4:
			return "shop";
		case 5:
			return null; // "?";
		case 6:
			return null; // "?";
		case 7:
			return "remote_work";
		case 8:
			return "leisure";
		case 9:
			return "escort_kids";
		case 10:
			return "escort_other";
		case 11:
			return "remote_home";
		default:
			return null;
		}
	}

	private String getMode(int modeIndex) {
		if (modeIndex >= 2 && modeIndex <= 8) {
			return "pt";
		} else if (modeIndex == 9) {
			return "car";
		} else if (modeIndex == 14) {
			return "bike";
		} else if (modeIndex == 15) {
			return "walk";
		} else {
			return null;
		}
	}
}
