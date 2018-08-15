package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items;

public class FreightTrafficODItem {
    int originZone;
    int destinationZone;
    double numberOfTrips;
    String freightType;

    public FreightTrafficODItem(int originZone, int destinationZone, double numberOfTrips, String freightType) {
        this.originZone = originZone;
        this.destinationZone = destinationZone;
        this.numberOfTrips = numberOfTrips;
        this.freightType = freightType;
    }

    public int getOriginZone() {
        return originZone;
    }

    public int getDestinationZone() {
        return destinationZone;
    }

    public double getNumberOfTrips() {
        return numberOfTrips;
    }

    public String getFreightType() {
        return freightType;
    }
}
