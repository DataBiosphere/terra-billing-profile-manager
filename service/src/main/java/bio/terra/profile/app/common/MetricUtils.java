package bio.terra.profile.app.common;

import bio.terra.profile.model.CloudPlatform;
import io.micrometer.core.instrument.Metrics;

public class MetricUtils {

  private final static String CLOUD_PLATFORM_TAG = "cloudPlatform";

  /**
   * Emit a metric for the duration of creating a billing profile.
   *
   * @param runnable the code to execute to create the profile, which will be timed
   * @param platform the platform the profile will be created on
   */
  public static void recordProfileCreation(Runnable runnable, CloudPlatform platform) {
    Metrics.globalRegistry
        .timer("profile.creation.time", CLOUD_PLATFORM_TAG, platform.toString())
        .record(runnable);
  }

  /**
   * Emit a metric for the number of profiles deleted.
   *
   * @param platform the platform of the profile that was deleted
   */
  public static void incrementProfileDeletion(CloudPlatform platform) {
    Metrics.globalRegistry
        .counter("profile.deletion.count", CLOUD_PLATFORM_TAG, platform.toString())
        .increment();
  }
}
