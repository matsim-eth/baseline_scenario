package ch.ethz.matsim.baseline_scenario.utils;

import ch.ethz.matsim.baseline_scenario.config.BaselineConfigGroup;
import ch.ethz.matsim.baseline_scenario.config.SwitzerlandConfig;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

import java.util.Arrays;

public class AdaptConfigForModeChoice {
	public void run(double scale, Config config) {
//		config.plans().setInputFile(scenarioConfig.prefix + "population.xml.gz");
//		config.network().setInputFile(scenarioConfig.prefix + "network.xml.gz");

		config.plansCalcRoute().getOrCreateModeRoutingParams("bike").setTeleportedModeSpeed(10.0 * 1000 / 3600.0);
		config.plansCalcRoute().getOrCreateModeRoutingParams("bike").setBeelineDistanceFactor(1.4);

		config.plansCalcRoute().getOrCreateModeRoutingParams("walk").setTeleportedModeSpeed(5.0 * 1000 / 3600.0);
		config.plansCalcRoute().getOrCreateModeRoutingParams("walk").setBeelineDistanceFactor(1.05);

		config.plansCalcRoute().getOrCreateModeRoutingParams("access_walk").setTeleportedModeSpeed(5.0 * 1000 / 3600.0);
		config.plansCalcRoute().getOrCreateModeRoutingParams("access_walk").setBeelineDistanceFactor(1.05);

		config.plansCalcRoute().getOrCreateModeRoutingParams("egress_walk").setTeleportedModeSpeed(5.0 * 1000 / 3600.0);
		config.plansCalcRoute().getOrCreateModeRoutingParams("egress_walk").setBeelineDistanceFactor(1.05);

//		DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
//		config.addModule(dvrpConfigGroup);
//		dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

		BaselineConfigGroup baselineConfigGroup = new BaselineConfigGroup();
		config.addModule(baselineConfigGroup);
		baselineConfigGroup.setCrossingPenality(3.0); // 3 seconds at intersection

		config.qsim().setFlowCapFactor(scale);
		config.qsim().setStorageCapFactor(1.0);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		config.qsim().setMainModes(Arrays.asList("car")); //, "prav"));
//		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.plansCalcRoute().setNetworkModes(Arrays.asList("car")); //, "prav"));
//		config.planCalcScore().getOrCreateModeParams("av"); // For mode share plot
//		config.planCalcScore().getOrCreateModeParams("prav");
//		config.subtourModeChoice().setModes(new String[] { "walk", "bike", "pt", "car", "av", "prav" });
		config.subtourModeChoice().setModes(new String[] { "walk", "bike", "pt", "car" });

//		config.travelTimeCalculator().setAnalyzedModes("car,prav");
		config.travelTimeCalculator().setAnalyzedModes("car");
		config.travelTimeCalculator().setSeparateModes(false);

		baselineConfigGroup.setScenarioScale(scale);
//		baselineConfigGroup.setEnablePrivateAVs(false);
//		baselineConfigGroup.setEnableSharedAVs(false);
		baselineConfigGroup.setScoringParametersPath("scoring.json");

//		AVPriceCalculationConfigGroup priceConfig = new AVPriceCalculationConfigGroup();
//		priceConfig.setInitialPrice(0.40);
//		priceConfig.setMode(AVPriceCalculationMode.FIXED);
//		config.addModule(priceConfig);

//		AVWaitingTimeCalculatorConfigGroup waitingTimeConfig = new AVWaitingTimeCalculatorConfigGroup();
//		config.addModule(waitingTimeConfig);
//		waitingTimeConfig.setCalculator(AVWaitingTimeCalculatorType.FIXED);

//		KernelWaitingTimeCalculatorConfigGroup kernelWaitingTimeConfig = new KernelWaitingTimeCalculatorConfigGroup();
//		config.addModule(kernelWaitingTimeConfig);

//		ZonalWaitingTimeCalculatorConfigGroup zonalWaitingTimeConfig = new ZonalWaitingTimeCalculatorConfigGroup();
//		config.addModule(zonalWaitingTimeConfig);

//		zonalWaitingTimeConfig.setBinSizeAsString("00:30:00");
//		zonalWaitingTimeConfig.setStartTimeAsString("05:00:00");
//		zonalWaitingTimeConfig.setEndTimeAsString("22:00:00");
//		zonalWaitingTimeConfig.setShapeFile("hexagon_750/hexagon_750.shp");
//		zonalWaitingTimeConfig.setZoneIdAttribute("hexid");
//
//		AVConfigGroup avConfig = new AVConfigGroup();
//		config.addModule(avConfig);
//
//		avConfig.setConfigPath("av_services.xml");


		// setup replanning

		config.strategy().clearStrategySettings();
		config.strategy().setMaxAgentPlanMemorySize(1);

		StrategySettings strategy;

		// See MATSIM-766 (https://matsim.atlassian.net/browse/MATSIM-766)
		strategy = new StrategySettings();
		strategy.setStrategyName("SubtourModeChoice");
		strategy.setDisableAfter(0);
		strategy.setWeight(0.0);
		config.strategy().addStrategySettings(strategy);

		strategy = new StrategySettings();
		strategy.setStrategyName("ASTRAModeChoice");
		strategy.setWeight(0.05);
		config.strategy().addStrategySettings(strategy);

		strategy = new StrategySettings();
		strategy.setStrategyName("ReRoute");
		strategy.setWeight(0.05);
		config.strategy().addStrategySettings(strategy);

		strategy = new StrategySettings();
		strategy.setStrategyName("KeepLastSelected");
		strategy.setWeight(0.9);
		config.strategy().addStrategySettings(strategy);

		config.linkStats().setWriteLinkStatsInterval(0);

		config.controler().setWritePlansInterval(0);
		config.controler().setWriteEventsInterval(20);
	}
}
