/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package ch.ethz.matsim.baseline_scenario.additional_traffic.freight;

import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.FreightTrafficODItem;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils.DepartureTimeGenerator;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils.FreightFacilitySelector;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils.SingleFreightTripUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Creates single-trip populations from OD-matrices.
 * @author tchervec
 *
 * Refactored from ivt_baseline/preparation/freightCreation/CreateFreightTraffic
 * @author boescpa
 *
 */
public class FreightTrafficCreator {
    private static final Logger log = Logger.getLogger(FreightTrafficCreator.class);
    private final Random random;
    private List<FreightTrafficODItem> freightTrafficODItems;
    private FreightFacilitySelector freightFacilitySelector;
    private DepartureTimeGenerator departureTimeGenerator;

    public FreightTrafficCreator(List<FreightTrafficODItem> freightTrafficODItems,
                                 FreightFacilitySelector freightFacilitySelector,
                                 DepartureTimeGenerator departureTimeGenerator,
                                 Random random) {
        this.freightTrafficODItems = freightTrafficODItems;
        this.freightFacilitySelector = freightFacilitySelector;
        this.departureTimeGenerator = departureTimeGenerator;
        this.random = random;
    }


    public void run(Population population, ActivityFacilities activityFacilities) {
        log.info("adding freight population and activity facilities");
        int personIndex = 0;

        Counter tripCounter = new Counter(" freight trip # ");
        Counter planCounter = new Counter(" freight plan # ");
        Counter facilityCounter = new Counter(" freight facility # ");

        for (FreightTrafficODItem freightTrafficODItem : freightTrafficODItems) {
            for (int i = 0; i < roundNumberOfTrips(freightTrafficODItem.getNumberOfTrips()); i++) {
                Id<Person> personId = Id.createPersonId("freight" + "_" + Integer.toString(++personIndex));
                Optional<ActivityFacility> startFacility = freightFacilitySelector.getFreightFacility(freightTrafficODItem.getOriginZone());
                Optional<ActivityFacility> endFacility = freightFacilitySelector.getFreightFacility(freightTrafficODItem.getDestinationZone());

                if (!startFacility.isPresent() || !endFacility.isPresent()) {
                    log.warn("Ignoring OD pair from " + freightTrafficODItem.getOriginName() +
                            " to " + freightTrafficODItem.getDestinationName());
                    continue;
                }

                double departureTime = departureTimeGenerator.getDepartureTime();
                Plan plan = SingleFreightTripUtils.createSingleTripPlan(departureTime, "freight",
                        TransportMode.car, startFacility.get(), endFacility.get());
                Person person = SingleFreightTripUtils.createSingleTripAgent(personId, freightTrafficODItem.getFreightType(), plan);

                population.addPerson(person);
                planCounter.incCounter();

                if (!activityFacilities.getFacilities().containsKey(startFacility.get().getId())){
                    activityFacilities.addActivityFacility(startFacility.get());
                    facilityCounter.incCounter();
                }
                if (!activityFacilities.getFacilities().containsKey(endFacility.get().getId())){
                    activityFacilities.addActivityFacility(endFacility.get());
                    facilityCounter.incCounter();
                }

                tripCounter.incCounter();
            }
        }
        tripCounter.printCounter();
        planCounter.printCounter();
        facilityCounter.printCounter();
    }

    private int roundNumberOfTrips(double numberOfTrips) {
        // first all full trips for this OD-relationship
        int totalNumberOfTrips = (int)Math.floor(numberOfTrips);
        // then - if chance allows - another trip
        double residualTrips = numberOfTrips - totalNumberOfTrips;
        if (random.nextDouble() <= residualTrips) {
            totalNumberOfTrips++;
        }
        // return the total number of trips
        return totalNumberOfTrips;
    }
}
