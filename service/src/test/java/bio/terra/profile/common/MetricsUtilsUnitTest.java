package bio.terra.profile.common;

import bio.terra.profile.app.common.MetricUtils;
import bio.terra.profile.model.CloudPlatform;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MetricsUtilsUnitTest extends BaseUnitTest {

  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    Metrics.globalRegistry.add(meterRegistry);
  }

  @AfterEach
  void tearDown() {
    meterRegistry.clear();
    Metrics.globalRegistry.clear();
  }

  @Test
  void createGcpProfileMetrics() {
    var profile = ProfileFixtures.createGcpBillingProfile("ABCD1234");

    MetricUtils.recordProfileCreation(() -> profile, profile.cloudPlatform());

    var timer = meterRegistry.find("bpm.profile.creation.time").timer();
    assertNotNull(timer);
    assertEquals(timer.count(), 1);
    assertEquals(timer.getId().getTag("cloudPlatform"), CloudPlatform.GCP.toString());
  }

  @Test
  void createAzureProfileMetrics() {
    var profile = ProfileFixtures.createAzureBillingProfile(UUID.randomUUID(), UUID.randomUUID(), "fake-mrg");

    MetricUtils.recordProfileCreation(() -> profile, CloudPlatform.AZURE);

    var timer = meterRegistry.find("bpm.profile.creation.time").timer();
    assertNotNull(timer);
    assertEquals(timer.count(), 1);
    assertEquals(timer.getId().getTag("cloudPlatform"), CloudPlatform.AZURE.toString());
  }

}
