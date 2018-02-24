package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.DefaultNetworkCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.DefaultTeleportationCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.DefaultTransitRouteCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.DefaultTransitTripCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.NetworkCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TeleportationCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TransitRouteCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TransitTripCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.CarTripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.ModeAwareTripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.PublicTransitTripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.TeleportationTripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips.TripProcessor;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DefaultDepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DepartureFinder;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class PlanCutterModule extends AbstractModule {
	final private TransitSchedule transitSchedule;

	public PlanCutterModule(TransitSchedule transitSchedule) {
		this.transitSchedule = transitSchedule;
	}

	@Override
	protected void configure() {
		// Needs @Named("road") Network
		// Needs ScenarioExtent

		// Needs StageActivityTypes
		// Needs MainModeIdentifier
	}

	@Provides
	@Singleton
	public CarTripProcessor provideCarTripProcessor(NetworkCrossingPointFinder networkCrossingPointFinder,
			ScenarioExtent extent) {
		return new CarTripProcessor(networkCrossingPointFinder, extent);
	}

	@Provides
	@Singleton
	public NetworkCrossingPointFinder provideNetworkCrossingPointFinder(@Named("road") Network network,
			ScenarioExtent extent) {
		return new DefaultNetworkCrossingPointFinder(extent, network, new FreeSpeedTravelTime());
	}

	@Provides
	@Singleton
	public TeleportationTripProcessor provideTeleportationTripProcessor(
			TeleportationCrossingPointFinder teleportationCrossingPointFinder, ScenarioExtent extent) {
		return new TeleportationTripProcessor(teleportationCrossingPointFinder, extent);
	}

	@Provides
	@Singleton
	public TeleportationCrossingPointFinder provideTeleportationCrossingPointFinder(ScenarioExtent extent) {
		return new DefaultTeleportationCrossingPointFinder(extent);
	}

	@Provides
	@Singleton
	public PublicTransitTripProcessor providePublicTransitTripProcessor(
			TransitTripCrossingPointFinder transitTripCrossingPointFinder, ScenarioExtent extent) {
		return new PublicTransitTripProcessor(transitTripCrossingPointFinder, extent, 1.0);
	}

	@Provides
	@Singleton
	public DepartureFinder provideDepartureFinder() {
		return new DefaultDepartureFinder();
	}

	@Provides
	@Singleton
	public TransitRouteCrossingPointFinder provideTransitRouteCrossingPointFinder(ScenarioExtent extent,
			DepartureFinder departureFinder) {
		return new DefaultTransitRouteCrossingPointFinder(extent, transitSchedule, departureFinder);
	}

	@Provides
	@Singleton
	public TransitTripCrossingPointFinder provideTransitTripCrossingPointFinder(
			TransitRouteCrossingPointFinder transitRouteCrossingPointFinder,
			TeleportationCrossingPointFinder teleportationCrossingPointFinder) {
		return new DefaultTransitTripCrossingPointFinder(transitRouteCrossingPointFinder,
				teleportationCrossingPointFinder);
	}

	@Provides
	@Singleton
	public TripProcessor provideTripProcessor(MainModeIdentifier mainModeIdentifier, CarTripProcessor carTripProcessor,
			TeleportationTripProcessor teleportationTripProcessor,
			PublicTransitTripProcessor publicTransitTripProcessor) {
		Map<String, TripProcessor> processors = new HashMap<>();

		processors.put("car", carTripProcessor);
		processors.put("pt", publicTransitTripProcessor);
		processors.put("bike", teleportationTripProcessor);
		processors.put("walk", teleportationTripProcessor);

		return new ModeAwareTripProcessor(mainModeIdentifier, processors);
	}

	@Provides
	@Singleton
	public PlanCutter providePlanCutter(TripProcessor tripProcessor, ScenarioExtent extent,
			StageActivityTypes stageActivityTypes) {
		return new PlanCutter(tripProcessor, extent, stageActivityTypes);
	}
}
