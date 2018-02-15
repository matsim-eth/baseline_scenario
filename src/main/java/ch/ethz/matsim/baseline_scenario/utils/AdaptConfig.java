package ch.ethz.matsim.baseline_scenario.utils;

import java.util.stream.Collectors;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ch.sbb.matsim.config.SBBTransitConfigGroup;

public class AdaptConfig {
	final private TransitSchedule transitSchedule;

	public AdaptConfig(TransitSchedule transitSchedule) {
		this.transitSchedule = transitSchedule;
	}

	public Config run(double scenarioScale, String prefix) {
		Config config = ConfigUtils.createConfig();

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.plans().setInputFile(prefix + "population.xml.gz");
		config.plans().setInputPersonAttributeFile(prefix + "population_attributes.xml.gz");
		config.facilities().setInputFile(prefix + "facilities.xml.gz");
		config.network().setInputFile(prefix + "network.xml.gz");
		config.households().setInputFile(prefix + "households.xml.gz");
		config.households().setInputHouseholdAttributesFile(prefix + "household_attributes.xml.gz");
		config.transit().setTransitScheduleFile(prefix + "transit_schedule.xml.gz");
		config.transit().setVehiclesFile(prefix + "transit_vehicles.xml.gz");

		config.transit().setUseTransit(true);

		config.controler().setOutputDirectory("simulation_output");

		config.global().setNumberOfThreads(8);
		config.qsim().setNumberOfThreads(8);

		config.qsim().setFlowCapFactor(scenarioScale);
		config.qsim().setStorageCapFactor(10000.0);
		config.qsim().setEndTime(30.0 * 3600.0);
		config.qsim().setSimEndtimeInterpretation(EndtimeInterpretation.onlyUseEndtime);

		config.strategy().clearStrategySettings();

		StrategySettings reroute = new StrategySettings();
		reroute.setStrategyName("ReRoute");
		reroute.setWeight(0.1);
		config.strategy().addStrategySettings(reroute);

		StrategySettings selection = new StrategySettings();
		selection.setStrategyName("ChangeExpBeta");
		selection.setWeight(0.9);
		config.strategy().addStrategySettings(selection);

		SBBTransitConfigGroup sbbTransitConfigGroup = new SBBTransitConfigGroup();
		sbbTransitConfigGroup.setDeterministicServiceModes(
				transitSchedule.getTransitLines().values().stream().flatMap(l -> l.getRoutes().values().stream())
						.map(r -> r.getTransportMode()).collect(Collectors.toSet()));
		config.addModule(sbbTransitConfigGroup);

		return config;
	}
}
