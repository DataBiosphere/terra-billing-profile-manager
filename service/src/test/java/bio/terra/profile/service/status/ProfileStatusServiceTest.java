package bio.terra.profile.service.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.model.SystemStatus;
import bio.terra.profile.model.SystemStatusSystems;
import bio.terra.profile.service.iam.SamService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ProfileStatusServiceTest extends BaseUnitTest {
  @Autowired private ProfileStatusService statusService;

  @MockBean private SamService mockSamService;

  // TODO add more cases when we have something to mock out

  @Test
  void testStatusWithWorkingEndpoints() {
    doReturn(new SystemStatusSystems().ok(true)).when(mockSamService).status();
    statusService.checkStatus();
    assertEquals(
        new SystemStatus()
            .ok(true)
            .putSystemsItem("CloudSQL", new SystemStatusSystems().ok(true))
            .putSystemsItem("Sam", new SystemStatusSystems().ok(true)),
        statusService.getCurrentStatus());
  }

  @Test
  void testFailureNotOk() {
    doReturn(new SystemStatusSystems().ok(false).messages(List.of("Sam failed")))
        .when(mockSamService)
        .status();
    statusService.checkStatus();
    assertEquals(
        new SystemStatus()
            .ok(false)
            .putSystemsItem("CloudSQL", new SystemStatusSystems().ok(true))
            .putSystemsItem(
                "Sam", new SystemStatusSystems().ok(false).messages(List.of("Sam failed"))),
        statusService.getCurrentStatus());
  }
}
