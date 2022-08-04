package bio.terra.profile.service.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.profile.app.configuration.StatusCheckConfiguration;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.model.SystemStatus;
import bio.terra.profile.model.SystemStatusSystems;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class BaseStatusServiceTest extends BaseSpringUnitTest {
  private static final int STALENESS = 7;
  private static final StatusCheckConfiguration configuration =
      new StatusCheckConfiguration(true, 5, 1, STALENESS);

  @Test
  void testSingleComponent() {
    BaseStatusService statusService = new BaseStatusService(configuration);
    statusService.registerStatusCheck("okcheck", this::okStatusCheck);
    statusService.checkStatus();
    assertEquals(
        new SystemStatus().ok(true).putSystemsItem("okcheck", okStatusCheck()),
        statusService.getCurrentStatus());

    statusService.registerStatusCheck("notokcheck", this::notOkStatusCheck);
    statusService.checkStatus();
    assertEquals(
        new SystemStatus()
            .ok(false)
            .putSystemsItem("okcheck", okStatusCheck())
            .putSystemsItem("notokcheck", notOkStatusCheck()),
        statusService.getCurrentStatus());
  }

  @Test
  void testStaleness() throws InterruptedException {
    BaseStatusService statusService = new BaseStatusService(configuration);
    statusService.registerStatusCheck("okcheck", this::okStatusCheck);
    statusService.checkStatus();
    TimeUnit.SECONDS.sleep(STALENESS + 2);
    assertEquals(new SystemStatus().ok(false), statusService.getCurrentStatus());
    statusService.checkStatus();
    assertEquals(
        new SystemStatus().ok(true).putSystemsItem("okcheck", okStatusCheck()),
        statusService.getCurrentStatus());
  }

  private SystemStatusSystems okStatusCheck() {
    return new SystemStatusSystems().ok(true);
  }

  private SystemStatusSystems notOkStatusCheck() {
    return new SystemStatusSystems().ok(false).addMessagesItem("not ok");
  }
}
