package ch.ethz.matsim.baseline_scenario.zurich.cutter.config;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class RunConfigCutter {
	static public void main(String[] args) {
		String configInputPath = args[0];
		String configOutputPath = args[1];
		
		Config config = ConfigUtils.loadConfig(configInputPath);
		new ConfigCutter("zurich_").run(config);
		new ConfigWriter(config).write(configOutputPath);
	}
}
