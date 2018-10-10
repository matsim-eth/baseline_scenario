package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.items;

public class FreightTrafficODItem {
    private int originZone;
    private String originName;
    private int destinationZone;
    private String destinationName;
    private double numberOfTrips;
    private String freightType;

    public FreightTrafficODItem(int originZone, String originName, int destinationZone, String destinationName, double numberOfTrips, String freightType) {
        this.originZone = originZone;
        this.originName = originName;
        this.destinationZone = destinationZone;
        this.destinationName = destinationName;
        this.numberOfTrips = numberOfTrips;
        this.freightType = freightType;
    }

    public int getOriginZone() {
        return originZone;
    }

    public String getOriginName() {
        return originName;
    }

    public int getDestinationZone() {
        return destinationZone;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public double getNumberOfTrips() {
        return numberOfTrips;
    }

    public String getFreightType() {
        return freightType;
    }
}
