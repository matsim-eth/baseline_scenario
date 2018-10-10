package ch.ethz.matsim.baseline_scenario.population;


import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;

import javax.inject.Inject;

public class PTransitAgentFactory implements AgentFactory {

    private final Netsim simulation;

    @Inject
    public PTransitAgentFactory(final Netsim simulation) {
        this.simulation = simulation;
    }

    @Override
    public MobsimDriverPassengerAgent createMobsimAgentFromPerson(final Person p) {
        MobsimDriverPassengerAgent agent = PTransitAgent.createTransitAgent(p, this.simulation);
        return agent;
    }
}
