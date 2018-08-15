package ch.ethz.matsim.baseline_scenario.additional_traffic.freight;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class FreightTraffic {
    private Population population;
    private ActivityFacilities facilities;

    public FreightTraffic(Population population, ActivityFacilities facilities) {
        this.population = population;
        this.facilities = facilities;
    }

    public Population getPopulation() {
        return population;
    }

    public ActivityFacilities getFacilities() {
        return facilities;
    }

    public FreightTraffic add(FreightTraffic freightTraffic) {
        for (Person person : freightTraffic.getPopulation().getPersons().values()) {
            this.population.addPerson(person);
        }

        for (ActivityFacility facility : freightTraffic.getFacilities().getFacilities().values()) {
            this.facilities.addActivityFacility(facility);
        }

        return this;
    }

}
