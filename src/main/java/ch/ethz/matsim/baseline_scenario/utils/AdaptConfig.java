package ch.ethz.matsim.baseline_scenario.utils;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

public class AdaptConfig {
	public Config run(double scenarioScale) {
		Config config = ConfigUtils.createConfig();
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.plans().setInputFile("output_population.xml.gz");
		config.plans().setInputPersonAttributeFile("output_population_attributes.xml.gz");
		config.facilities().setInputFile("output_facilities.xml.gz");
		config.network().setInputFile("output_network.xml.gz");
		
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
		
		return config;
	}
}
