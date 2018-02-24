package ch.ethz.matsim.baseline_scenario.zurich.cutter.population;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import ch.ethz.matsim.baseline_scenario.zurich.consistency.ActivityCheck;
import ch.ethz.matsim.baseline_scenario.zurich.consistency.BatchCheck;
import ch.ethz.matsim.baseline_scenario.zurich.consistency.ChainStructureCheck;
import ch.ethz.matsim.baseline_scenario.zurich.consistency.PlanConsistencyCheck;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.connector.OutsideConnector;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.PlanCutter;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.PlanCutterModule;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.DefaultMergeOutsideActivities;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.MergeOutsideActivities;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.utils.RemoveEmptyPlans;
import ch.ethz.matsim.baseline_scenario.zurich.extent.CircularScenarioExtent;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class RunPopulationCutter {
	static public void main(String[] args) throws InterruptedException, ExecutionException {
		String populationInputPath = args[0];
		String networkInputPath = args[1];
		String facilitiesInputPath = args[2];
		String transitScheduleInputPath = args[3];
		String populationOutputPath = args[4];
		String facilitiesOutputPath = args[5];
		String networkOutputPath = args[6];

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationInputPath);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkInputPath);
		new MatsimFacilitiesReader(scenario).readFile(facilitiesInputPath);
		new TransitScheduleReader(scenario).readFile(transitScheduleInputPath);

		Coord bellevue = new Coord(2683253.0, 1246745.0);
		ScenarioExtent extent = new CircularScenarioExtent(scenario.getNetwork(), bellevue, 30000.0);

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));

		Link referenceLink = NetworkUtils.getNearestLink(roadNetwork, bellevue);

		Injector injector = Guice.createInjector(new PlanCutterModule(scenario.getTransitSchedule()),
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(ScenarioExtent.class).toInstance(extent);
						bind(TransitSchedule.class).toInstance(scenario.getTransitSchedule());
						bind(Key.get(Network.class, Names.named("road"))).toInstance(roadNetwork);
						bind(Key.get(Link.class, Names.named("reference"))).toInstance(referenceLink);
						bind(StageActivityTypes.class)
								.toInstance(new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
						bind(MainModeIdentifier.class).toInstance(new MainModeIdentifierImpl());
						bind(ActivityFacilities.class).toInstance(scenario.getActivityFacilities());
					}
				});

		MergeOutsideActivities mergeOutsideActivities = new DefaultMergeOutsideActivities();

		ParallelPopulationCutter populationCutter = new DefaultParallelPopulationCutter(
				injector.getInstance(PlanCutter.class), mergeOutsideActivities);

		ExecutorService executor = Executors.newFixedThreadPool(4);
		populationCutter.run(scenario.getPopulation(), executor);
		executor.shutdown();

		MergeOutsideActivities merger = new DefaultMergeOutsideActivities();
		scenario.getPopulation().getPersons().values().forEach(p -> merger.run(p.getSelectedPlan().getPlanElements()));

		OutsideConnector outsideConnector = new OutsideConnector(scenario.getPopulation());
		outsideConnector.run(scenario.getActivityFacilities(), scenario.getNetwork(), roadNetwork);

		new RemoveEmptyPlans().run(scenario.getPopulation());

		PlanConsistencyCheck planConsistencyCheck = new BatchCheck(
				new ChainStructureCheck(extent, scenario.getNetwork()),
				new ActivityCheck(scenario.getNetwork(), scenario.getActivityFacilities()));
		scenario.getPopulation().getPersons().values()
				.forEach(p -> planConsistencyCheck.run(p.getSelectedPlan().getPlanElements()));

		new PopulationWriter(scenario.getPopulation()).write(populationOutputPath);
		new FacilitiesWriter(scenario.getActivityFacilities()).write(facilitiesOutputPath);
		new NetworkWriter(scenario.getNetwork()).write(networkOutputPath);
	}
}
