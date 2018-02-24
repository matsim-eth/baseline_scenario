package ch.ethz.matsim.baseline_scenario.zurich.router.trip;

import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import ch.ethz.matsim.baseline_scenario.zurich.utils.ActivityWithFacility;

public class CarTripRouter implements TripRouter {
	final private TripRouterWithRoutingModule delegate;

	public CarTripRouter(Network network) {
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);

		LeastCostPathCalculatorFactory pathCalculatorFactory = new DijkstraFactory();
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(network, travelDisutility,
				travelTime);

		this.delegate = new TripRouterWithRoutingModule(
				new NetworkRoutingModule("car", PopulationUtils.getFactory(), network, pathCalculator));
	}

	@Override
	public List<PlanElement> route(ActivityWithFacility originActivity, List<PlanElement> trip,
			ActivityWithFacility destinationActivity) {
		return delegate.route(originActivity, trip, destinationActivity);
	}

}
