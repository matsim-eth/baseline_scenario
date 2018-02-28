package ch.ethz.matsim.baseline_scenario.zurich.router.parallel;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import ch.ethz.matsim.baseline_scenario.zurich.utils.ActivityWithFacility;
import ch.ethz.matsim.baseline_scenario.zurich.utils.DefaultActivityWithFacility;

public class DefaultParallelPlanRouter implements ParallelPlanRouter {
	final private ParallelTripRouter tripRouter;
	final private StageActivityTypes stageActivityTypes;
	final private ActivityFacilities activityFacilities;

	public DefaultParallelPlanRouter(ParallelTripRouter tripRouter, StageActivityTypes stageActivityTypes,
			ActivityFacilities activityFacilities) {
		this.stageActivityTypes = stageActivityTypes;
		this.tripRouter = tripRouter;
		this.activityFacilities = activityFacilities;
	}

	@Override
	public CompletableFuture<List<PlanElement>> route(List<PlanElement> plan, Executor executor) {
		List<CompletableFuture<List<PlanElement>>> futures = new LinkedList<>();

		for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan, stageActivityTypes)) {
			if (futures.size() == 0) {
				futures.add(CompletableFuture.completedFuture(Collections.singletonList(trip.getOriginActivity())));
			}

			ActivityFacility originFacility = activityFacilities.getFacilities()
					.get(trip.getOriginActivity().getFacilityId());
			ActivityFacility destinationFacility = activityFacilities.getFacilities()
					.get(trip.getDestinationActivity().getFacilityId());

			ActivityWithFacility originActivity = new DefaultActivityWithFacility(trip.getOriginActivity(),
					originFacility);
			ActivityWithFacility destinationActivity = new DefaultActivityWithFacility(trip.getDestinationActivity(),
					destinationFacility);

			futures.add(tripRouter.route(originActivity, trip.getTripElements(),
					(ActivityWithFacility) destinationActivity, executor));
			futures.add(CompletableFuture.completedFuture(Collections.singletonList(trip.getDestinationActivity())));
		}

		CompletableFuture<?> temporary[] = new CompletableFuture<?>[futures.size()];
		CompletableFuture<Void> allFuture = CompletableFuture.allOf(futures.toArray(temporary));

		return allFuture.thenApply(o -> {
			List<PlanElement> result = new LinkedList<>();

			futures.forEach(f -> {
				try {
					result.addAll(f.get());
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			});

			return result;
		});
	}
}
