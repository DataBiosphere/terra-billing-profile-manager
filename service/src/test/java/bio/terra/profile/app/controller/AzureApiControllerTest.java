package bio.terra.profile.app.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.iam.SamService;
import java.util.UUID;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public class AzureApiControllerTest extends BaseSpringUnitTest {

  @Autowired MockMvc mockMvc;

  @MockBean SamService samService;
  @MockBean AzureService azureService;
  private final AuthenticatedUserRequest userRequest =
      AuthenticatedUserRequest.builder()
          .setEmail("example@example.com")
          .setSubjectId("fake_sub")
          .setToken("fake_token")
          .build();

  @BeforeEach
  void setup() throws Exception {
    when(samService.getUserStatusInfo(eq(userRequest.getToken())))
        .thenReturn(
            new UserStatusInfo()
                .userSubjectId(userRequest.getSubjectId())
                .userEmail(userRequest.getEmail())
                .enabled(true));
  }

  @Test
  void getManagedApps_returnsOk() throws Exception {
    var subId = UUID.randomUUID();
    mockMvc
        .perform(
            get("/api/azure/v1/managedApps")
                .queryParam("azureSubscriptionId", subId.toString())
                .header("Authorization", "Bearer " + userRequest.getToken()))
        .andExpect(status().is(HttpStatus.OK.value()));
  }

  @Test
  void getManagedApps_returnsBadRequestWithNoUUID() throws Exception {
    mockMvc
        .perform(
            get("/api/azure/v1/managedApps")
                .header("Authorization", "Bearer " + userRequest.getToken()))
        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
  }

  @Test
  void getManagedApps_returnsBadRequestWithBadUUID() throws Exception {
    mockMvc
        .perform(
            get("/api/azure/v1/managedApps")
                .queryParam("azureSubscriptionId", "baduuid")
                .header("Authorization", "Bearer " + userRequest.getToken()))
        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    assert (false);
  }
}
