package ch.ethz.matsim.baseline_scenario.analysis.counts.utils.compatibility;

import ch.ethz.matsim.baseline_scenario.analysis.counts.items.CountItem;
import ch.ethz.matsim.baseline_scenario.analysis.counts.items.HourlyCountItem;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class CountNetworkMapperTest {

    @Test
    public void run() {

        // create 2 single-link networks where only the link id changes
        Network oldNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
        oldNetwork.addNode(oldNetwork.getFactory().createNode(Id.create("node0", Node.class), new Coord((double) 0, (double) 0)));
        oldNetwork.addNode(oldNetwork.getFactory().createNode(Id.create("node1", Node.class), new Coord((double) 100, (double) 0)));
        oldNetwork.addLink(oldNetwork.getFactory().createLink(Id.create( "oldLinkId", Link.class),
                oldNetwork.getNodes().get(Id.create("node0", Node.class)),
                oldNetwork.getNodes().get(Id.create("node1", Node.class))));

        Network newNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
        newNetwork.addNode(newNetwork.getFactory().createNode(Id.create("node0", Node.class), new Coord((double) 0, (double) 0)));
        newNetwork.addNode(newNetwork.getFactory().createNode(Id.create("node1", Node.class), new Coord((double) 100, (double) 0)));
        newNetwork.addLink(newNetwork.getFactory().createLink(Id.create( "newLinkId", Link.class),
                newNetwork.getNodes().get(Id.create("node0", Node.class)),
                newNetwork.getNodes().get(Id.create("node1", Node.class))));

        List<CountItem> countItems = new LinkedList<>();
        for (int hour=0; hour<24; hour++) {
            countItems.add(new HourlyCountItem(Id.create( "oldLinkId", Link.class), hour, 10, new Coord(0.0, 0.0), "station1"));
        }

        new CountNetworkMapper(oldNetwork, newNetwork).run(countItems);

        assertEquals("Wrong number of final mapped count items", 24, countItems.size());
        for (CountItem countItem : countItems) {
            assertEquals(countItem.link.toString(),"newLinkId");
        }
    }
}