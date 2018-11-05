package ch.ethz.matsim.baseline_scenario.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

import com.opencsv.CSVIterator;
import com.opencsv.CSVReader;

public class MergeSecondaryFacilities {
	final private String activityType;
	final private String sourcePath;
	final private Network network;
	final private double probability;
	final private Random random;

	public MergeSecondaryFacilities(Random random, String activityType, String sourcePath, double probability,
			Network network) {
		this.activityType = activityType;
		this.sourcePath = sourcePath;
		this.network = network;
		this.probability = probability;
		this.random = random;
	}

	public void run(ActivityFacilities facilities) throws IOException {
		CSVIterator iterator = new CSVIterator(
				new CSVReader(new InputStreamReader(new FileInputStream(sourcePath)), ';'));
		iterator.next();

		long currentId = 0;

		while (iterator.hasNext()) {
			String[] row = iterator.next();

			if (random.nextDouble() <= probability) {
				Id<ActivityFacility> facilityId = Id.create(String.format("%s_%d", activityType, currentId++),
						ActivityFacility.class);
				Coord coord = new Coord(Double.parseDouble(row[1]), Double.parseDouble(row[2]));
				double capacity = Double.parseDouble(row[6]);
				OpeningTime openingTime = new OpeningTimeImpl(Double.parseDouble(row[7]), Double.parseDouble(row[8]));

				Link link = NetworkUtils.getNearestLink(network, coord);
				ActivityFacility facility = facilities.getFactory().createActivityFacility(facilityId, coord,
						link.getId());

				ActivityOption activityOption = new ActivityOptionImpl(activityType);
				activityOption.setCapacity(capacity);
				activityOption.addOpeningTime(openingTime);

				facility.addActivityOption(activityOption);
				facilities.addActivityFacility(facility);
			}
		}
	}
}
