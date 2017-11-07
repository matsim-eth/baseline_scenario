package ch.ethz.matsim.baseline_scenario.analysis.simulation;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

import com.google.inject.Singleton;

@Singleton
public class ModeShareListener implements IterationEndsListener {
	final private Population population;
	final private OutputDirectoryHierarchy hierarchy;

	final private StageActivityTypes stageActivityTypes;
	final private MainModeIdentifier mainModeIdentifier;
	
	public ModeShareListener(Population population, OutputDirectoryHierarchy hierarchy, StageActivityTypes stageActivityTypes, MainModeIdentifier mainModeIdentifier) {
		this.population = population;
		this.hierarchy = hierarchy;
		this.stageActivityTypes = stageActivityTypes;
		this.mainModeIdentifier = mainModeIdentifier;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			Map<String, AtomicInteger> counts = new TreeMap<>();

			for (Person person : population.getPersons().values()) {
				Plan selectedPlan = person.getSelectedPlan();

				List<String> modes = TripStructureUtils.getTrips(selectedPlan, stageActivityTypes).stream()
						.map(trip -> mainModeIdentifier.identifyMainMode(trip.getTripElements()))
						.collect(Collectors.toList());

				for (String mode : modes) {
					if (!counts.containsKey(mode)) {
						counts.put(mode, new AtomicInteger(0));
					}

					counts.get(mode).incrementAndGet();
				}
			}

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(hierarchy.getIterationFilename(event.getIteration(), "mode_share.txt"))));

			for (Map.Entry<String, AtomicInteger> entry : counts.entrySet()) {
				writer.write(String.format("%s %d\n", entry.getKey(), entry.getValue().get()));
				writer.flush();
			}

			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}