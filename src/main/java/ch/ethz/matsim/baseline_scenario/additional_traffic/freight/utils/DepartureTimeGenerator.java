package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.utils;

import java.util.Random;

public class DepartureTimeGenerator {
    private Random random;
    private final double[] cumulativeDepartureProbability;

    public DepartureTimeGenerator(Random random, double[] cumulativeDepartureProbability) {
        this.random = random;
        this.cumulativeDepartureProbability = cumulativeDepartureProbability;
    }

    public int getDepartureTime() {
        double randDep = random.nextDouble();
        // identify selected hour of day
        int hour = 0;
        while (hour < 23 && cumulativeDepartureProbability[hour + 1] < randDep) {
            hour++;
        }
        int time = hour*60*60;
        // random assignment within that hour of the day
        time += random.nextInt(3600);
        return time;
    }
}
