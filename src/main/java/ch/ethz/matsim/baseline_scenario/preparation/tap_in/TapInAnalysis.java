package ch.ethz.matsim.baseline_scenario.preparation.tap_in;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.ethz.matsim.baseline_scenario.config.CommandLine;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;
import ch.ethz.matsim.baseline_scenario.transit.events.PublicTransitEvent;
import ch.ethz.matsim.baseline_scenario.transit.events.PublicTransitEventMapper;

public class TapInAnalysis {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("population", "schedule", "events", "output") //
				.allowOptions("lda") //
				.build();

		String populationPath = cmd.getOptionStrict("population");
		String schedulePath = cmd.getOptionStrict("schedule");
		String eventsPath = cmd.getOptionStrict("events");
		String outputPath = cmd.getOptionStrict("output");
		Optional<String> ldaPath = cmd.getOption("lda");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationPath);
		new TransitScheduleReader(scenario).readFile(schedulePath);

		EventsManager eventsManager = EventsUtils.createEventsManager();

		if (ldaPath.isPresent()) {
			Map<String, Set<Id<TransitStopFacility>>> mapping = new HashMap<>();

			for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
				Id<TransitStopFacility> facilityId = facility.getId();
				String stopId = facilityId.toString().split(".link:")[0];

				if (!mapping.containsKey(stopId)) {
					mapping.put(stopId, new HashSet<>());
				}

				mapping.get(stopId).add(facilityId);
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ldaPath.get())));

			List<String> header = null;
			String line = null;

			while ((line = reader.readLine()) != null) {
				List<String> segments = Arrays.asList(line.split(";"));

				if (header == null) {
					header = segments;
				} else {
					int lda = Integer.parseInt(segments.get(header.indexOf("lda")));
					String stopId = segments.get(header.indexOf("stop_id"));

					if (mapping.containsKey(stopId)) {
						for (Id<TransitStopFacility> facilityId : mapping.get(stopId)) {
							scenario.getTransitSchedule().getFacilities().get(facilityId).getAttributes()
									.putAttribute("LDA", lda);
						}
					}
				}
			}

			reader.close();

			TapInListener tapInListener = new TapInListener(eventsManager, scenario.getTransitSchedule(),
					scenario.getPopulation());
			eventsManager.addHandler(tapInListener);
		}

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		Listener listener = new Listener(writer);
		eventsManager.addHandler(listener);

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
		reader.addCustomEventMapper(PublicTransitEvent.TYPE, new PublicTransitEventMapper());
		reader.readFile(eventsPath);
		writer.close();
	}

	static private class Listener implements GenericEventHandler {
		private final BufferedWriter writer;

		public Listener(BufferedWriter writer) throws IOException {
			this.writer = writer;

			writer.write("time;person;lda;subscription\n");
			writer.flush();
		}

		public void handleEvent(TapInEvent event) {
			try {
				writer.write(
						String.join(";",
								new String[] { String.valueOf(event.getTime()), event.getPersonId().toString(),
										String.valueOf(event.getLDA()), String.valueOf(event.hasSubscription()) })
								+ "\n");
				writer.flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void handleEvent(GenericEvent event) {
			if (event instanceof TapInEvent) {
				handleEvent((TapInEvent) event);
			}
		}
	}
}
