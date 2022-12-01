package bio.terra.profile.service.profile.flight.delete;

import static org.mockito.Mockito.*;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.azure.ApplicationService;
import bio.terra.profile.service.profile.flight.MRGTags;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class UnlinkBillingProfileIdFromMrgStepUnitTest extends BaseUnitTest {

  @Test
  void unlinkingProfileStepRemovesTagFromMRG() throws Exception {
    var tenantId = UUID.randomUUID();
    var subscriptionId = UUID.randomUUID();
    var mrgId = "test-MRG-ID";

    var profile =
        new BillingProfile(
            UUID.randomUUID(),
            "fake-bp-name",
            "fake-description",
            "direct",
            CloudPlatform.AZURE,
            Optional.of("ABCDEF-1234"),
            Optional.of(tenantId),
            Optional.of(subscriptionId),
            Optional.of(mrgId),
            null,
            null,
            null);
    ApplicationService applicationService = mock(ApplicationService.class);

    var step = new UnlinkBillingProfileIdFromMrgStep(applicationService, profile);

    var flightContext = mock(FlightContext.class);
    step.doStep(flightContext);

    verify(applicationService)
        .removeTagFromMrg(tenantId, subscriptionId, mrgId, MRGTags.BILLING_PROFILE_ID);
  }

  @Test
  void undoStepForAzureProfileRelinksMRG() throws Exception {
    var tenantId = UUID.randomUUID();
    var subscriptionId = UUID.randomUUID();
    var mrgId = "test-MRG-ID";

    var profile =
        new BillingProfile(
            UUID.randomUUID(),
            "fake-bp-name",
            "fake-description",
            "direct",
            CloudPlatform.AZURE,
            Optional.of("ABCDEF-1234"),
            Optional.of(tenantId),
            Optional.of(subscriptionId),
            Optional.of(mrgId),
            null,
            null,
            null);
    ApplicationService applicationService = mock(ApplicationService.class);

    var step = new UnlinkBillingProfileIdFromMrgStep(applicationService, profile);

    var flightContext = mock(FlightContext.class);
    step.undoStep(flightContext);

    verify(applicationService)
        .addTagToMrg(
            tenantId, subscriptionId, mrgId, MRGTags.BILLING_PROFILE_ID, profile.id().toString());
  }
}
