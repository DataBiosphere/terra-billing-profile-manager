package bio.terra.profile.service.profile.flight.delete;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.azure.ApplicationService;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.profile.ProfileService;
import bio.terra.profile.service.profile.model.BillingProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class DeleteProfileFlightTest extends BaseSpringUnitTest {

  @Autowired
  ProfileService profileService;
  @Autowired
  AzureConfiguration azureConfiguration;
  @MockBean
  CrlService crlService;

  @MockBean
  ProfileDao profileDao;
  @MockBean
  SamService samService;
  @MockBean
  AzureService azureService;
  @MockBean
  ApplicationService applicationService;

  AuthenticatedUserRequest userRequest =
      AuthenticatedUserRequest.builder()
          .setToken("fake-token")
          .setSubjectId("fake-sub")
          .setEmail("example@example.com")
          .build();


  @Test
  void deletingAzureProfileUnlinksMRG() {
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
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);

    profileService.deleteProfile(profile.id(), userRequest);
    verify(applicationService).removeTagFromMrg(tenantId, subscriptionId, mrgId, "terra.billingProfileId");
  }

  @Test
  void deletingGCPProfileDoesNotDoUnlinkMRGStep() {
    var profile =
        new BillingProfile(
            UUID.randomUUID(),
            "fake-bp-name",
            "fake-description",
            "direct",
            CloudPlatform.GCP,
            Optional.of("ABCDEF-1234"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            null,
            null,
            null);
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);

    profileService.deleteProfile(profile.id(), userRequest);
    verify(applicationService, never()).removeTagFromMrg(any(), any(), any(), any());

  }


}
