package bio.terra.profile.service.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseIntegrationTest;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.profile.model.BillingProfile;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AzureProfileCreationIntegrationTest extends BaseIntegrationTest {

  @Autowired private ProfileService profileService;

  @MockBean private SamService samService;

  @Test
  void createAzureBillingProfile() throws InterruptedException {
    var token = "fakeToken";
    var userRequest =
        AuthenticatedUserRequest.builder()
            .setToken(token)
            .setEmail("harry.potter@test.firecloud.org")
            .setSubjectId("sub")
            .build();
    BillingProfile azureBillingProfile =
        new BillingProfile(
            UUID.randomUUID(),
            "azureBillingProfile-" + UUID.randomUUID(),
            "Example azure billing profile",
            "direct",
            CloudPlatform.AZURE,
            Optional.empty(),
            Optional.of(UUID.fromString("fad90753-2022-4456-9b0a-c7e5b934e408")),
            Optional.of(UUID.fromString("df547342-9cfd-44ef-a6dd-df0ede32f1e3")),
            Optional.of("terraIntegrationTesting"),
            null,
            null,
            null);
    var createdProfile = profileService.createProfile(azureBillingProfile, userRequest);
    assertEquals(createdProfile.id(), azureBillingProfile.id());

    verify(samService).createManagedResourceGroup(any(), eq(userRequest));
  }
}
