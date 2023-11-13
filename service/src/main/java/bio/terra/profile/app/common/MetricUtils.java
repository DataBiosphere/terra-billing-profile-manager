package bio.terra.profile.app.common;

import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.profile.model.ProfileDescription;
import io.micrometer.core.instrument.Metrics;
import java.util.concurrent.Callable;

public class MetricUtils {

  private static final String NAMESPACE = "bpm";
  private static final String CLOUD_PLATFORM_TAG = "cloudPlatform";

  private MetricUtils() {}

  /**
   * Emit a metric for the duration of creating a billing profile.
   *
   * @param callable the code to execute to create the profile, which will be timed
   * @param platform the platform the profile will be created on
   */
  public static ProfileDescription recordProfileCreation(
      Callable<ProfileDescription> callable, CloudPlatform platform) {
    try {
      return Metrics.globalRegistry
          .timer(
              String.format("%s.profile.creation.time", NAMESPACE),
              CLOUD_PLATFORM_TAG,
              platform.toString())
          .recordCallable(callable);
    } catch (RuntimeException ex) {
      // The method we are calling does not throw checked exceptions, so retain the original.
      throw ex;
    } catch (Exception ex) {
      // We should not get into this case, see above.
      throw new RuntimeException(ex);
    }
  }

  /**
   * Emit a metric for the number of profiles deleted.
   *
   * @param platform the platform of the profile that was deleted
   */
  public static void incrementProfileDeletion(CloudPlatform platform) {
    Metrics.globalRegistry
        .counter(
            String.format("%s.profile.deletion.count", NAMESPACE),
            CLOUD_PLATFORM_TAG,
            platform.toString())
        .increment();
  }
}
