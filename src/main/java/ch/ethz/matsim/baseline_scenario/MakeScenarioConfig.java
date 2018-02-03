package ch.ethz.matsim.baseline_scenario;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ch.ethz.matsim.baseline_scenario.config.BaselineConfig;

public class MakeScenarioConfig {
	static public void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper json = new ObjectMapper();
		json.enable(SerializationFeature.INDENT_OUTPUT);
		json.writeValue(new File(args[0]), new BaselineConfig());
	}
}
