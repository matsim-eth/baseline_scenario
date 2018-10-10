package ch.ethz.matsim.baseline_scenario.population;

import com.google.inject.Module;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;

import java.util.Collection;
import java.util.Collections;

public class PPopulationPlugin extends AbstractQSimPlugin {

    public PPopulationPlugin(Config config) {
        super(config);
    }

    @Override
    public Collection<? extends Module> modules() {
        return Collections.singletonList(new com.google.inject.AbstractModule() {
            @Override
            protected void configure() {
                bind(PopulationAgentSource.class).asEagerSingleton();
                if (getConfig().transit().isUseTransit()) {
                    bind(AgentFactory.class).to(PTransitAgentFactory.class).asEagerSingleton();
                } else {
                    bind(AgentFactory.class).to(DefaultAgentFactory.class).asEagerSingleton();
                }
            }
        });
    }

    @Override
    public Collection<Class<? extends AgentSource>> agentSources() {
        return Collections.singletonList(PopulationAgentSource.class);
    }
}