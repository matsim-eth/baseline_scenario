package ch.ethz.matsim.baseline_scenario.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

public class CommandLine {
	final private static Logger logger = Logger.getLogger(CommandLine.class);

	final private static String CONFIG_PREFIX = "config";
	final private static String FLAG_VALUE = "true";

	final private Set<String> allowedPrefixes;
	final private Set<String> allowedOptions;
	final private Set<String> requiredOptions;

	final private Map<String, String> options = new HashMap<>();
	final private List<String> positionalArguments = new LinkedList<>();

	final private boolean positionalArgumentsAllowed;
	final private boolean allowAnyOption;

	// Configuration part

	static public class Builder {
		final private Set<String> allowedPrefixes = new HashSet<>(Collections.singleton(CONFIG_PREFIX));
		final private Set<String> allowedOptions = new HashSet<>();
		final private Set<String> requiredOptions = new HashSet<>();

		private boolean positionalArgumentsAllowed = true;
		private boolean allowAnyOption = false;

		final private List<String> arguments;

		public Builder(String[] args) {
			this.arguments = Arrays.asList(args);
		}

		public Builder allowPositionalArguments(boolean allow) {
			positionalArgumentsAllowed = allow;
			return this;
		}

		public Builder allowAnyOption(boolean allow) {
			allowAnyOption = allow;
			return this;
		}

		public Builder allowOptions(Collection<String> options) {
			allowedOptions.addAll(options);
			return this;
		}

		public Builder allowOptions(String... options) {
			allowOptions(Arrays.asList(options));
			return this;
		}

		public Builder requireOptions(Collection<String> options) {
			allowedOptions.addAll(options);
			requiredOptions.addAll(options);
			return this;
		}

		public Builder requireOptions(String... options) {
			requireOptions(Arrays.asList(options));
			return this;
		}

		public Builder allowPrefixes(Collection<String> prefixes) {
			allowedPrefixes.addAll(prefixes);
			return this;
		}

		public Builder allowPrefixes(String... prefixes) {
			allowPrefixes(Arrays.asList(prefixes));
			return this;
		}

		public CommandLine build() throws ConfigurationException {
			CommandLine commandLine = new CommandLine(allowedOptions, requiredOptions, allowedPrefixes,
					positionalArgumentsAllowed, allowAnyOption);
			commandLine.process(arguments);
			return commandLine;
		}
	}

	private CommandLine(Set<String> allowedOptions, Set<String> requiredOptions, Set<String> allowedPrefixes,
			boolean positionalArgumentsAllowed, boolean allowAnyOption) {
		this.allowedOptions = allowedOptions;
		this.requiredOptions = requiredOptions;
		this.allowedPrefixes = allowedPrefixes;
		this.positionalArgumentsAllowed = positionalArgumentsAllowed;
		this.allowAnyOption = allowAnyOption;
	}

	// Getters for positional arguments

	public int getNumberOfPositionalArguments() {
		return positionalArguments.size();
	}

	public List<String> getPositionalArguments() {
		return Collections.unmodifiableList(positionalArguments);
	}

	public Optional<String> getPositionalArgument(int index) {
		return index < positionalArguments.size() ? Optional.of(positionalArguments.get(index)) : Optional.empty();
	}

	public String getPositionalArgumentStrict(int index) throws ConfigurationException {
		if (index < positionalArguments.size()) {
			return positionalArguments.get(index);
		} else {
			throw new ConfigurationException(String.format(
					"Requested positional command line argument with index %d, but only %d arguments are available",
					index, positionalArguments.size()));
		}
	}

	// Getters for options

	public Collection<String> getAvailableOptions() {
		return Collections.unmodifiableCollection(options.keySet());
	}

	public boolean hasOption(String option) {
		return options.containsKey(option);
	}

	public Optional<String> getOption(String option) {
		return options.containsKey(option) ? Optional.of(options.get(option)) : Optional.empty();
	}

	public String getOptionStrict(String option) throws ConfigurationException {
		if (options.containsKey(option)) {
			return options.get(option);
		} else {
			throw new ConfigurationException(
					String.format("Requested command line option '%s' is not available", option));
		}
	}

	// Processing

	private void process(List<String> args) throws ConfigurationException {
		List<String> arguments = flattenArguments(args);
		positionalArguments.clear();

		String currentOption = null;

		for (String token : arguments) {
			if (token.startsWith("--")) {
				if (currentOption != null) {
					addOption(currentOption, FLAG_VALUE);
				}

				currentOption = token.substring(2);
			} else {
				if (currentOption != null) {
					addOption(currentOption, token);
				} else {
					addPositionalArgument(token);
				}

				currentOption = null;
			}
		}

		if (currentOption != null) {
			addOption(currentOption, FLAG_VALUE);
		}

		checkRequiredOptions();
		reportOptions();
	}

	private List<String> flattenArguments(List<String> args) {
		List<String> flatArguments = new LinkedList<>();

		for (String argument : args) {
			int index = argument.indexOf("=");
			int bracketIndex = argument.indexOf("]");

			if (bracketIndex > index) {
				index = argument.indexOf("=", bracketIndex);
			}

			if (index > -1) {
				flatArguments.add(argument.substring(0, index));
				flatArguments.add(argument.substring(index + 1));
			} else {
				flatArguments.add(argument);
			}
		}

		return flatArguments;
	}

	private void addPositionalArgument(String value) throws ConfigurationException {
		if (!positionalArgumentsAllowed) {
			throw new ConfigurationException(String.format("Positional argument '%s' is not allowed.", value));
		}

		positionalArguments.add(value);
	}

	private void addOption(String option, String value) throws ConfigurationException {
		if (!allowAnyOption && !allowedOptions.contains(option)) {
			String[] parts = option.split(":");

			if (!allowedPrefixes.contains(parts[0])) {
				throw new ConfigurationException(String.format("Option '%s' is not allowed.", option));
			}
		}

		options.put(option, value);
	}

	private void checkRequiredOptions() throws ConfigurationException {
		List<String> missingOptions = new LinkedList<>();

		for (String option : requiredOptions) {
			if (!options.containsKey(option)) {
				missingOptions.add(option);
			}
		}

		if (missingOptions.size() > 0) {
			throw new ConfigurationException(
					String.format("The following options are missing: %s", missingOptions.toString()));
		}
	}

	public void applyConfiguration(Config config) throws ConfigurationException {
		List<String> configOptions = options.keySet().stream().filter(o -> o.startsWith(CONFIG_PREFIX + ":"))
				.collect(Collectors.toList());

		for (String option : configOptions) {
			processConfigOption(config, option, option.substring(CONFIG_PREFIX.length() + 1));
		}
	}

	private void reportOptions() {
		logger.info(String.format("Received %d positional command line arguments:", positionalArguments.size()));
		logger.info("   " + String.join(" , ", positionalArguments));

		Map<String, List<String>> prefixedOptions = new HashMap<>();
		List<String> nonPrefixedOptions = new LinkedList<>();

		for (String option : options.keySet()) {
			int separatorIndex = option.indexOf(":");

			if (separatorIndex > -1) {
				String prefix = option.substring(0, separatorIndex);
				option = option.substring(separatorIndex + 1);

				if (!prefixedOptions.containsKey(prefix)) {
					prefixedOptions.put(prefix, new LinkedList<>());
				}

				prefixedOptions.get(prefix).add(option);
			} else {
				nonPrefixedOptions.add(option);
			}
		}

		// prefixedOptions.remove("config");

		logger.info(String.format("Received %d command line options with %d prefixes:", options.size(),
				prefixedOptions.size()));

		Collections.sort(nonPrefixedOptions);
		for (String option : nonPrefixedOptions) {
			logger.info(String.format("   %s = %s", option, options.get(option)));
		}

		List<String> orderedPrefixes = new LinkedList<>(prefixedOptions.keySet());
		Collections.sort(orderedPrefixes);

		for (String prefix : orderedPrefixes) {
			logger.info(String.format("   Prefix %s:", prefix));

			for (String option : prefixedOptions.get(prefix)) {
				logger.info(String.format("      %s = %s", option, options.get(prefix + ":" + option)));
			}
		}
	}

	private void processConfigOption(Config config, String option, String remainder) throws ConfigurationException {
		int separatorIndex = remainder.indexOf(".");

		if (separatorIndex > -1) {
			String module = remainder.substring(0, separatorIndex);
			String newRemainder = remainder.substring(separatorIndex + 1);

			if (config.getModules().containsKey(module)) {
				processParameter(option, module, config.getModules().get(module), newRemainder);
			} else {
				throw new ConfigurationException(
						String.format("Invalid MATSim option: '%s'. Module '%s' does not exist.", remainder, module));
			}
		} else {
			throw new ConfigurationException(
					String.format("Malformatted MATSim option: '%s'. Expected MODULE.*", remainder));
		}
	}

	private void processParameter(String option, String path, ConfigGroup configGroup, String remainder)
			throws ConfigurationException {
		if (remainder.contains("[")) {
			int selectorStartIndex = remainder.indexOf("[");
			int selectorEndIndex = remainder.indexOf("]");
			int equalIndex = remainder.indexOf("=");

			if (selectorStartIndex > -1 && selectorEndIndex > -1 && equalIndex > -1) {
				if (selectorStartIndex < equalIndex && equalIndex < selectorEndIndex) {
					String parameterSetType = remainder.substring(0, selectorStartIndex);
					String selectionParameter = remainder.substring(selectorStartIndex + 1, equalIndex);
					String selectionValue = remainder.substring(equalIndex + 1, selectorEndIndex);

					String newRemainder = remainder.substring(selectorEndIndex + 1);

					if (newRemainder.startsWith(".")) {
						newRemainder = newRemainder.substring(1);

						String newPath = String.format("%s.%s[%s=%s]", path, parameterSetType, selectionParameter,
								selectionValue);

						Collection<? extends ConfigGroup> parameterSets = configGroup
								.getParameterSets(parameterSetType);

						if (parameterSets.size() > 0) {
							for (ConfigGroup parameterSet : parameterSets) {
								if (parameterSet.getParams().containsKey(selectionParameter)) {
									String comparisonValue = parameterSet.getParams().get(selectionParameter);

									if (comparisonValue.equals(selectionValue)) {
										processParameter(option, newPath, parameterSet, newRemainder);
										return;
									}
								}
							}

							throw new ConfigurationException(
									String.format("Parameter set '%s' with %s=%s for %s is not available in %s",
											parameterSetType, selectionParameter, selectionValue, path, option));
						} else {
							throw new ConfigurationException(
									String.format("Parameter set of type '%s' for %s is not available in %s",
											parameterSetType, path, option));
						}
					}
				}
			}

			throw new ConfigurationException(String.format(
					"Malformatted parameter set selector: '%s' in %s. Expected %s.SET_TYPE[PARAM=VALUE].*", remainder,
					option, path));
		} else {
			if (configGroup.getParams().containsKey(remainder)) {
				String value = options.get(option);
				configGroup.addParam(remainder, value);
				logger.info(String.format("Setting %s to %s", path, value));
			} else {
				throw new ConfigurationException(String.format("Parameter %s in %s is not available", remainder, path));
			}
		}
	}

	// Exception

	static public class ConfigurationException extends Exception {
		public ConfigurationException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 8427111111975754721L;
	}
}
