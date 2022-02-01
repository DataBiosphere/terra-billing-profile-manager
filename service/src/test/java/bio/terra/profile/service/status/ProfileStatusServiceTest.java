package bio.terra.profile.service.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.generated.model.ApiSystemStatus;
import bio.terra.profile.generated.model.ApiSystemStatusSystems;
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
    doReturn(new ApiSystemStatusSystems().ok(true)).when(mockSamService).status();
    statusService.checkStatus();
    assertEquals(
        new ApiSystemStatus()
            .ok(true)
            .putSystemsItem("CloudSQL", new ApiSystemStatusSystems().ok(true))
            .putSystemsItem("Sam", new ApiSystemStatusSystems().ok(true)),
        statusService.getCurrentStatus());
  }

  @Test
  void testFailureNotOk() {
    doReturn(new ApiSystemStatusSystems().ok(false).messages(List.of("Sam failed")))
        .when(mockSamService)
        .status();
    statusService.checkStatus();
    assertEquals(
        new ApiSystemStatus()
            .ok(false)
            .putSystemsItem("CloudSQL", new ApiSystemStatusSystems().ok(true))
            .putSystemsItem(
                "Sam", new ApiSystemStatusSystems().ok(false).messages(List.of("Sam failed"))),
        statusService.getCurrentStatus());
  }
}
