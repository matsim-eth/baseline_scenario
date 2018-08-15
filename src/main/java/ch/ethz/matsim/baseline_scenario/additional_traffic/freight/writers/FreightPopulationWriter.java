package ch.ethz.matsim.baseline_scenario.additional_traffic.freight.writers;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class FreightPopulationWriter {
    private final Population population;

    public FreightPopulationWriter(final Population population) {
        this.population = population;
    }

    public void write(String outputPath) {
        PopulationWriter writer = new PopulationWriter(population);
        writer.write(outputPath);
        ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(population.getPersonAttributes());
        attributesWriter.writeFile(outputPath.substring(0, outputPath.indexOf(".xml")) + "_attributes.xml.gz");
    }
}
