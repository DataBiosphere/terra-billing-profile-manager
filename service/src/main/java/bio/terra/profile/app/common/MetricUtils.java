package bio.terra.profile.app.common;

import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.profile.model.BillingProfile;
import io.micrometer.core.instrument.Metrics;
import java.util.concurrent.Callable;

public class MetricUtils {

  private static final String CLOUD_PLATFORM_TAG = "cloudPlatform";

  /**
   * Emit a metric for the duration of creating a billing profile.
   *
   * @param callable the code to execute to create the profile, which will be timed
   * @param platform the platform the profile will be created on
   */
  public static BillingProfile recordProfileCreation(
      Callable<BillingProfile> callable, CloudPlatform platform) {
    try {
      return Metrics.globalRegistry
          .timer("profile.creation.time", CLOUD_PLATFORM_TAG, platform.toString())
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
        .counter("profile.deletion.count", CLOUD_PLATFORM_TAG, platform.toString())
        .increment();
  }
}
