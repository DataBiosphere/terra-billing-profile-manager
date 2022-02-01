package bio.terra.profile.service.status;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.profile.app.configuration.StatusCheckConfiguration;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.generated.model.ApiSystemStatus;
import bio.terra.profile.generated.model.ApiSystemStatusSystems;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class BaseStatusServiceTest extends BaseUnitTest {
  private static final int STALENESS = 7;
  private static final StatusCheckConfiguration configuration =
      new StatusCheckConfiguration(true, 5, 1, STALENESS);

  @Test
  void testSingleComponent() {
    BaseStatusService statusService = new BaseStatusService(configuration);
    statusService.registerStatusCheck("okcheck", this::okStatusCheck);
    statusService.checkStatus();
    assertEquals(
        new ApiSystemStatus().ok(true).putSystemsItem("okcheck", okStatusCheck()),
        statusService.getCurrentStatus());

    statusService.registerStatusCheck("notokcheck", this::notOkStatusCheck);
    statusService.checkStatus();
    assertEquals(
        new ApiSystemStatus()
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
    assertEquals(new ApiSystemStatus().ok(false), statusService.getCurrentStatus());
    statusService.checkStatus();
    assertEquals(
        new ApiSystemStatus().ok(true).putSystemsItem("okcheck", okStatusCheck()),
        statusService.getCurrentStatus());
  }

  private ApiSystemStatusSystems okStatusCheck() {
    return new ApiSystemStatusSystems().ok(true);
  }

  private ApiSystemStatusSystems notOkStatusCheck() {
    return new ApiSystemStatusSystems().ok(false).addMessagesItem("not ok");
  }
}
