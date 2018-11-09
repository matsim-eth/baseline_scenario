package ch.ethz.matsim.baseline_scenario.transit;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class TransitModuleTest {
    @Rule
    public final MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testWorksWithTransitDisabled() {
        final Config config = utils.createConfig();
        config.transit().setUseTransit(false);
        config.controler().setLastIteration(0);

        final Controler controler = new Controler(ScenarioUtils.createScenario(config));
        controler.addOverridingModule(new SwissRailRaptorModule());
        controler.addOverridingModule(new BaselineTransitModule());
        controler.run();
    }
}
