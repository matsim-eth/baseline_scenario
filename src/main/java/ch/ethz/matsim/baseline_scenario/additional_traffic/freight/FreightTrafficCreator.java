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

import ch.ethz.matsim.baseline_scenario.additional_traffic.AdditionalTrafficType;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items.FreightTrafficODItem;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils.FreightFacilitySelector;
import ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils.SingleFreightTripUtils;
import ch.ethz.matsim.baseline_scenario.additional_traffic.utils.DepartureTimeGenerator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;

import java.util.List;
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
    private final Random random;
    private final double percentage;
    private final double scalingFactor;

    private List<FreightTrafficODItem> freightTrafficODItems;
    private FreightFacilitySelector freightFacilitySelector;
    private DepartureTimeGenerator departureTimeGenerator;


    private int personIndex = 0;

    public FreightTrafficCreator(Random random, double scalingFactor,
                                 List<FreightTrafficODItem> freightTrafficODItems,
                                 FreightFacilitySelector freightFacilitySelector,
                                 DepartureTimeGenerator departureTimeGenerator) {
        this.random = random;
        this.freightTrafficODItems = freightTrafficODItems;
        this.freightFacilitySelector = freightFacilitySelector;
        this.departureTimeGenerator = departureTimeGenerator;

        if (scalingFactor <= 1.0) {
            this.percentage = scalingFactor;
            this.scalingFactor = 1.0;
        } else {
            this.percentage = 1.0;
            this.scalingFactor = scalingFactor;
        }
    }


    public FreightTraffic create() {
        // TODO: HOW THE HELL DO YOU INITIALIZED A POPULATION OTHERWISE WITHOUT GOING BY SCENARIO?
        // PATRICK USED PopulationUtils.getEmptyPopulation(); but that no longer exits
        Population freightPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
        ActivityFacilities freightFacilities = FacilitiesUtils.createActivityFacilities();

        for (FreightTrafficODItem freightTrafficODItem : freightTrafficODItems) {
            for (int i = 0; i < roundNumberOfTrips(freightTrafficODItem.getNumberOfTrips()); i++) {
                if (random.nextDouble() <= percentage) {

                    Id<Person> personId = Id.createPersonId(AdditionalTrafficType.FREIGHT.toString() + "_" + Integer.toString(++personIndex));
                    ActivityFacility startFacility = freightFacilitySelector.getFreightFacility(freightTrafficODItem.getOriginZone());
                    ActivityFacility endFacility = freightFacilitySelector.getFreightFacility(freightTrafficODItem.getDestinationZone());

                    double departureTime = departureTimeGenerator.getDepartureTime();
                    Plan plan = SingleFreightTripUtils.createSingleTripPlan(departureTime, AdditionalTrafficType.FREIGHT.toString(),
                            TransportMode.car, startFacility, endFacility);
                    Person person = SingleFreightTripUtils.createSingleTripAgent(personId, freightTrafficODItem.getFreightType(), plan);

                    freightPopulation.addPerson(person);
                    freightFacilities.addActivityFacility(startFacility);
                    freightFacilities.addActivityFacility(endFacility);
                }
            }
        }
        return new FreightTraffic(freightPopulation, freightFacilities);
    }

    private int roundNumberOfTrips(double numberOfTrips) {
        // first scale number of trips
        double scaledNumberOfTrips = this.scalingFactor * numberOfTrips;
        // first all full trips for this OD-relationship
        int totalNumberOfTrips = (int)Math.floor(scaledNumberOfTrips);
        // then - if chance allows - another trip
        double residualTrips = scaledNumberOfTrips - totalNumberOfTrips;
        if (random.nextDouble() <= residualTrips) {
            totalNumberOfTrips++;
        }
        // return the total number of trips
        return totalNumberOfTrips;
    }
}
