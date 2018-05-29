package ch.ethz.matsim.baseline_scenario.scoring;

import ch.ethz.matsim.baseline_scenario.config.BaselineConfigGroup;
import ch.ethz.matsim.mode_choice.selectors.OldPlanForRemovalSelector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.ScoringFunctionFactory;

import java.io.File;
import java.io.IOException;

public class ASTRAScoringModule extends AbstractModule {
	final private Logger logger = Logger.getLogger(ASTRAScoringModule.class);

	@Override
	public void install() {
		bind(ASTRAScoringFunctionFactory.class).asEagerSingleton();
		bind(ScoringFunctionFactory.class).to(ASTRAScoringFunctionFactory.class);
		// bindPlanSelectorForRemoval().to(ASTRARemovalSelector.class);
		// addControlerListenerBinding().to(ASTRARemovalPreparation.class);

		bindPlanSelectorForRemoval().to(OldPlanForRemovalSelector.class);
	}

	@Provides
	@Singleton
	public ASTRAScoringParameters provideASTRALegScoringParameters(Config config, BaselineConfigGroup baselineConfigGroup) {
		String scoringParametersPath = baselineConfigGroup.getScoringParametersPath();
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		if (null == scoringParametersPath) {
			logger.info("Using standard scoring parameters");
			return new ASTRAScoringParameters();
		} else {
			logger.info("Using scoring parameters from " + scoringParametersPath);

			File inputFile = new File(
					ConfigGroup.getInputFileURL(config.getContext(), scoringParametersPath).getFile());

			if (!inputFile.exists()) {
				try {
					mapper.writeValue(inputFile, new ASTRAScoringParameters());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				throw new IllegalStateException("Parameter input file '" + scoringParametersPath
						+ "' did not exist! Created the default one at the same location.");
			} else {
				try {
					return mapper.readValue(inputFile, ASTRAScoringParameters.class);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

//	@Provides
//	public ASTRARemovalSelector provideASTRARemovalSelector() {
//		PlanSelector<Plan, Person> delegate = new RandomPlanSelector<>();
//		return new ASTRARemovalSelector(delegate);
//	}
}
