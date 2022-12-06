package bio.terra.profile.service.profile.flight.delete;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.common.ProfileFixtures;
import bio.terra.profile.service.iam.SamService;
import bio.terra.stairway.FlightContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class UnlinkBillingProfileIdFromMrgStepUnitTest extends BaseUnitTest {
  AuthenticatedUserRequest userRequest =
      AuthenticatedUserRequest.builder()
          .setToken("fake-token")
          .setSubjectId("fake-sub")
          .setEmail("example@example.com")
          .build();

  @Test
  void unlinkingProfileStepRemovesTagFromMRG() throws Exception {
    var tenantId = UUID.randomUUID();
    var subscriptionId = UUID.randomUUID();
    var mrgId = "test-MRG-ID";

    var profile = ProfileFixtures.createAzureBillingProfile(tenantId, subscriptionId, mrgId);
    SamService samService = mock(SamService.class);

    var step = new UnlinkBillingProfileIdFromMrgStep(samService, profile, userRequest);

    var flightContext = mock(FlightContext.class);
    step.doStep(flightContext);

    verify(samService).deleteManagedResourceGroup(profile.id(), userRequest);
  }

  @Test
  void undoStepForAzureProfileRelinksMRG() throws Exception {
    var tenantId = UUID.randomUUID();
    var subscriptionId = UUID.randomUUID();
    var mrgId = "test-MRG-ID";

    var profile = ProfileFixtures.createAzureBillingProfile(tenantId, subscriptionId, mrgId);
    SamService samService = mock(SamService.class);

    var step = new UnlinkBillingProfileIdFromMrgStep(samService, profile, userRequest);

    var flightContext = mock(FlightContext.class);
    step.undoStep(flightContext);

    verify(samService).createManagedResourceGroup(profile, userRequest);
  }
}
