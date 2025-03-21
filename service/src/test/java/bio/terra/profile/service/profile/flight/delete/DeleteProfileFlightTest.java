package bio.terra.profile.service.profile.flight.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.common.ProfileFixtures;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.azure.AzureService;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.policy.TpsApiDispatch;
import bio.terra.profile.service.profile.ProfileService;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class DeleteProfileFlightTest extends BaseSpringUnitTest {

  @Autowired ProfileService profileService;
  @Autowired AzureConfiguration azureConfiguration;
  @MockitoBean GcpCrlService crlService;

  @MockitoBean ProfileDao profileDao;
  @MockitoBean SamService samService;
  @MockitoBean AzureService azureService;
  @MockitoBean TpsApiDispatch tpsApiDispatch;

  AuthenticatedUserRequest userRequest =
      AuthenticatedUserRequest.builder()
          .setToken("fake-token")
          .setSubjectId("fake-sub")
          .setEmail("example@example.com")
          .build();

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
  void deletingAzureProfileUnlinksMRG() throws InterruptedException {
    var tenantId = UUID.randomUUID();
    var subscriptionId = UUID.randomUUID();
    var mrgId = "test-MRG-ID";

    var profile = ProfileFixtures.createAzureBillingProfile(tenantId, subscriptionId, mrgId);
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);

    profileService.deleteProfile(profile.id(), userRequest, null);
    verify(samService).deleteManagedResourceGroup(profile.id(), userRequest);
  }

  @Test
  void deletingGCPProfileDoesNotDoUnlinkMRGStep() throws InterruptedException {
    var profile = ProfileFixtures.createGcpBillingProfile("fake-gcp-id");
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);

    profileService.deleteProfile(profile.id(), userRequest, null);
    verify(samService, never()).deleteManagedResourceGroup(any(), any());
  }

  @Test
  void deletingProfileEmitsMetric() {
    var azureBillingProfile =
        ProfileFixtures.createAzureBillingProfile(
            UUID.randomUUID(), UUID.randomUUID(), "test-MRG-ID");
    when(profileDao.getBillingProfileById(azureBillingProfile.id()))
        .thenReturn(azureBillingProfile);

    var gcpBillingProfile = ProfileFixtures.createGcpBillingProfile("fake-gcp-id");
    when(profileDao.getBillingProfileById(gcpBillingProfile.id())).thenReturn(gcpBillingProfile);

    profileService.deleteProfile(azureBillingProfile.id(), userRequest, null);
    profileService.deleteProfile(gcpBillingProfile.id(), userRequest, null);
    profileService.deleteProfile(gcpBillingProfile.id(), userRequest, null);

    var azureCounter =
        meterRegistry
            .find("bpm.profile.deletion.count")
            .tags("cloudPlatform", CloudPlatform.AZURE.toString())
            .counter();
    assertNotNull(azureCounter);
    assertEquals(1, azureCounter.count());

    var gcpCounter =
        meterRegistry
            .find("bpm.profile.deletion.count")
            .tags("cloudPlatform", CloudPlatform.GCP.toString())
            .counter();
    assertNotNull(gcpCounter);
    assertEquals(2, gcpCounter.count());
  }

  @Test
  void deletingProfileDeletesPolicies() throws InterruptedException {
    var tenantId = UUID.randomUUID();
    var subscriptionId = UUID.randomUUID();
    var mrgId = "test-MRG-ID";

    var profile = ProfileFixtures.createAzureBillingProfile(tenantId, subscriptionId, mrgId);
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);

    profileService.deleteProfile(profile.id(), userRequest, null);
    verify(tpsApiDispatch).deletePao(profile.id());
  }
}
