package ch.ethz.matsim.baseline_scenario.config;

//import ch.ethz.matsim.projects.astra.prav.PravAvailabilityWriter;
import org.matsim.core.config.ReflectiveConfigGroup;

public class BaselineConfigGroup extends ReflectiveConfigGroup {
	final public static String GROUP_NAME = "baseline";

	final public static String CROSSING_PENALITY = "crossingPenalty";
	final public static String FREESPEED_FACTOR = "freespeedFactor";
	final public static String SCORING_PARAMETERS_PATH = "scoringParametersPath";
	final public static String SCENARIO_SCALE = "scenarioScale";
	final public static String BASELINE = "baseline";
	final public static String ANALYSIS_REQUEST_PATH = "analysisRequestPath";
	final static String FLOW_EFFICIENCY_FACTOR = "flowEfficiencyFactor";

//	final public static String ENABLE_PRIVATE_AVS = "enablePrivateAVs";
//	final public static String ENABLE_SHARED_AVS = "enableSharedAVs";
//
//	final public static String NO_PRAV_AT_BORDER = "noPravAtBorder";
//	final public static String PRAV_AVAILABILITY = "pravAvailability";

	final public static String WRITE_TRIPS_INTERVAL = "writeTripsInterval";

	private double crossingPenality = 0.0;
	private double freespeedFactor = 1.0;
	private String scoringParametersPath = null;
	private double scenarioScale = Double.NaN;
	private boolean baseline = false;
	private String analysisRequestPath = "analysis_requests.txt";
//	private boolean enablePrivateAVs = false;
//	private boolean enableSharedAVs = true;
	private double flowEfficiencyFactor = 1.0;
//	private boolean noPravAtBorder = false;
	private int writeTripsInterval = 20;
//	private PravAvailabilityWriter.Mode pravAvailability = PravAvailabilityWriter.Mode.PERSON_CAR_AVAILABILITY;

	public BaselineConfigGroup() {
		super(GROUP_NAME);
	}

	@StringSetter(FLOW_EFFICIENCY_FACTOR)
	public void setFlowEfficiencyFactor(double flowEfficiencyFactor) {
		this.flowEfficiencyFactor = flowEfficiencyFactor;
	}

	@StringGetter(FLOW_EFFICIENCY_FACTOR)
	public double getFlowEfficiencyFactor() {
		return flowEfficiencyFactor;
	}

	@StringGetter(CROSSING_PENALITY)
	public double getCrossingPenality() {
		return crossingPenality;
	}

	@StringSetter(CROSSING_PENALITY)
	public void setCrossingPenality(double crossingPenality) {
		this.crossingPenality = crossingPenality;
	}

	@StringGetter(FREESPEED_FACTOR)
	public double getFreespeedFactor() {
		return freespeedFactor;
	}

	@StringSetter(FREESPEED_FACTOR)
	public void setFreespeedFactor(double freespeedFactor) {
		this.freespeedFactor = freespeedFactor;
	}

	@StringGetter(SCORING_PARAMETERS_PATH)
	public String getScoringParametersPath() {
		return scoringParametersPath;
	}

	@StringSetter(SCORING_PARAMETERS_PATH)
	public void setScoringParametersPath(String scoringParametersPath) {
		this.scoringParametersPath = scoringParametersPath;
	}

	@StringGetter(SCENARIO_SCALE)
	public double getScenarioScale() {
		return scenarioScale;
	}

	@StringSetter(SCENARIO_SCALE)
	public void setScenarioScale(double scenarioScale) {
		this.scenarioScale = scenarioScale;
	}

	@StringGetter(BASELINE)
	public boolean isBaseline() {
		return baseline;
	}

	@StringSetter(BASELINE)
	public void setBaseline(boolean baseline) {
		this.baseline = baseline;
	}

	@StringGetter(ANALYSIS_REQUEST_PATH)
	public String getAnalysisRequestPath() {
		return analysisRequestPath;
	}

	@StringSetter(ANALYSIS_REQUEST_PATH)
	public void setAnalysisRequestPath(String analysisRequestPath) {
		this.analysisRequestPath = analysisRequestPath;
	}

//	@StringSetter(ENABLE_PRIVATE_AVS)
//	public void setEnablePrivateAVs(boolean enablePrivateAVs) {
//		this.enablePrivateAVs = enablePrivateAVs;
//	}
//
//	@StringGetter(ENABLE_PRIVATE_AVS)
//	public boolean getEnablePrivateAVs() {
//		return enablePrivateAVs;
//	}
//
//	@StringSetter(ENABLE_SHARED_AVS)
//	public void setEnableSharedAVs(boolean enableSharedAVs) {
//		this.enableSharedAVs = enableSharedAVs;
//	}
//
//	@StringGetter(ENABLE_SHARED_AVS)
//	public boolean getEnableSharedAVs() {
//		return enableSharedAVs;
//	}
//
//	@StringSetter(NO_PRAV_AT_BORDER)
//	public void setNoPravAtBorder(boolean noPravAtBorder) {
//		this.noPravAtBorder = noPravAtBorder;
//	}
//
//	@StringGetter(NO_PRAV_AT_BORDER)
//	public boolean getNoPravAtBorder() {
//		return noPravAtBorder;
//	}

//	@StringSetter(PRAV_AVAILABILITY)
//	public void setPravAvailability(PravAvailabilityWriter.Mode pravAvailability) {
//		this.pravAvailability = pravAvailability;
//	}
//
//	@StringGetter(PRAV_AVAILABILITY)
//	public PravAvailabilityWriter.Mode getPravAvailability() {
//		return pravAvailability;
//	}

	@StringSetter(WRITE_TRIPS_INTERVAL)
	public void setWriteTripsInterval(int writeTripsInterval) {
		this.writeTripsInterval = writeTripsInterval;
	}

	@StringGetter(WRITE_TRIPS_INTERVAL)
	public int getWriteTripsInterval() {
		return writeTripsInterval;
	}
}
