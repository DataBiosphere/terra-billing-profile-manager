package bio.terra.profile.service.job;

import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.common.stairway.MonitoringHook;
import bio.terra.common.stairway.StairwayComponent;
import bio.terra.profile.service.job.exception.InvalidJobIdException;
import bio.terra.profile.service.job.exception.InvalidJobParameterException;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class JobBuilder {
  private final JobService jobService;
  private final StairwayComponent stairwayComponent;
  private final FlightMap jobParameterMap;
  private final OpenTelemetry openTelemetry;
  @Nullable private Class<? extends Flight> flightClass;
  @Nullable private String jobId;
  @Nullable private String description;
  @Nullable private Object request;
  @Nullable private AuthenticatedUserRequest userRequest;

  public JobBuilder(
      JobService jobService, StairwayComponent stairwayComponent, OpenTelemetry openTelemetry) {
    this.jobService = jobService;
    this.stairwayComponent = stairwayComponent;
    this.openTelemetry = openTelemetry;
    this.jobParameterMap = new FlightMap();
  }

  public JobBuilder flightClass(Class<? extends Flight> flightClass) {
    this.flightClass = flightClass;
    return this;
  }

  public JobBuilder jobId(@Nullable String jobId) {
    // If clients provide a non-null job ID, it cannot be whitespace-only
    if (StringUtils.isWhitespace(jobId)) {
      throw new InvalidJobIdException("jobId cannot be whitespace-only.");
    }
    this.jobId = jobId;
    return this;
  }

  public JobBuilder description(@Nullable String description) {
    this.description = description;
    return this;
  }

  public JobBuilder request(@Nullable Object request) {
    this.request = request;
    return this;
  }

  public JobBuilder userRequest(@Nullable AuthenticatedUserRequest userRequest) {
    this.userRequest = userRequest;
    return this;
  }

  public JobBuilder addParameter(String keyName, @Nullable Object val) {
    if (StringUtils.isBlank(keyName)) {
      throw new InvalidJobParameterException("Parameter name cannot be null or blanks.");
    }
    // note that this call overwrites a parameter if it already exists
    jobParameterMap.put(keyName, val);
    return this;
  }

  /**
   * Submit a job to stairway and return the jobID immediately.
   *
   * @return jobID of submitted flight
   */
  public String submit() {
    populateInputParams();
    return jobService.submit(flightClass, jobParameterMap, jobId);
  }

  /**
   * Submit a job to stairway, wait until it's complete, and return the job result.
   *
   * @param resultClass Class of the job's result
   * @return Result of the finished job.
   */
  @WithSpan
  public <T> T submitAndWait(Class<T> resultClass) {
    populateInputParams();
    return jobService.submitAndWait(flightClass, jobParameterMap, resultClass, jobId);
  }

  // Check the inputs, supply defaults and finalize the input parameter map
  private void populateInputParams() {
    if (flightClass == null) {
      throw new MissingRequiredFieldException("Missing flight class: flightClass");
    }

    // Default to a generated job id
    if (jobId == null) {
      jobId = stairwayComponent.get().createFlightId();
    }

    // Always add the tracing span parameters
    addParameter(
        MonitoringHook.SUBMISSION_SPAN_CONTEXT_MAP_KEY,
        MonitoringHook.serializeCurrentTracingContext(openTelemetry));

    // Convert the any other members that were set into parameters. However, if they were
    // explicitly added with addParameter during construction, we do not overwrite them.
    if (shouldInsert(JobMapKeys.DESCRIPTION, description)) {
      addParameter(JobMapKeys.DESCRIPTION.getKeyName(), description);
    }
    if (shouldInsert(JobMapKeys.REQUEST, request)) {
      addParameter(JobMapKeys.REQUEST.getKeyName(), request);
    }
    if (shouldInsert(JobMapKeys.AUTH_USER_INFO, userRequest)) {
      addParameter(JobMapKeys.AUTH_USER_INFO.getKeyName(), userRequest);
      addParameter(JobMapKeys.SUBJECT_ID.getKeyName(), userRequest.getSubjectId());
    }
  }

  private boolean shouldInsert(JobMapKeys mapKey, @Nullable Object value) {
    return (value != null && !jobParameterMap.containsKey(mapKey.getKeyName()));
  }
}
