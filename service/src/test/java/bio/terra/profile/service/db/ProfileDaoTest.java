package bio.terra.profile.service.db;

import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.profile.exception.DuplicateManagedApplicationException;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import bio.terra.profile.service.profile.exception.ProfileNotFoundException;
import bio.terra.profile.service.profile.model.BillingProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

class ProfileDaoTest extends BaseSpringUnitTest {

  @Autowired private ProfileDao profileDao;
  private AuthenticatedUserRequest user;
  private List<UUID> profileIds;

  @BeforeEach
  public void before() {
    profileIds = new ArrayList<>();
    user =
        AuthenticatedUserRequest.builder()
            .setSubjectId("12345")
            .setEmail("profile@unit.com")
            .setToken("token")
            .build();
  }

  @AfterEach
  public void teardown() throws Exception {
    for (UUID profileId : profileIds) {
      profileDao.deleteBillingProfileById(profileId);
    }
  }

  @Test
  void createAndGetProfile() {
    var profile = makeGCPProfile();
    var createResult = profileDao.createBillingProfile(profile, user);
    assertProfileEquals(profile, createResult);
    var getResult = profileDao.getBillingProfileById(profile.id());
    assertProfileEquals(profile, getResult);
  }

  @Test
  void createProfile_alreadyExists() {
    var profile = makeGCPProfile();
    var createResult = profileDao.createBillingProfile(profile, user);
    assertProfileEquals(profile, createResult);
    assertThrows(DuplicateKeyException.class, () -> profileDao.createBillingProfile(profile, user));
  }

  @Test
  void createProfile_duplicateManagedAppCoords() {
    UUID tenantId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();
    String managedResourceGroupId = "managedResourceGroupId";

    UUID profileId = UUID.randomUUID();
    profileIds.add(profileId);
    var profile = makeAzureProfile(tenantId, subscriptionId, managedResourceGroupId);

    var createResult = profileDao.createBillingProfile(profile, user);
    assertProfileEquals(profile, createResult);

    UUID duplicatedProfileId = UUID.randomUUID();
    profileIds.add(duplicatedProfileId);
    var duplicatedManagedAppCoordsProfile =
        new BillingProfile(
            duplicatedProfileId,
            "get your own managed app",
            "",
            "direct",
            CloudPlatform.AZURE,
            Optional.empty(),
            Optional.of(tenantId),
            Optional.of(subscriptionId),
            Optional.of(managedResourceGroupId),
            null,
            null,
            null);

    assertThrows(
        DuplicateManagedApplicationException.class,
        () -> profileDao.createBillingProfile(duplicatedManagedAppCoordsProfile, user));

    var differentMRGProfile =
        makeAzureProfile(tenantId, subscriptionId, "new_managed_resource_group");
    assertDoesNotThrow(() -> profileDao.createBillingProfile(differentMRGProfile, user));

    var differentSubscriptionProfile =
        makeAzureProfile(tenantId, UUID.randomUUID(), managedResourceGroupId);
    assertDoesNotThrow(() -> profileDao.createBillingProfile(differentSubscriptionProfile, user));
  }

  @Test
  void createAndDeleteProfile() {
    var profile = makeGCPProfile();
    var createResult = profileDao.createBillingProfile(profile, user);
    assertProfileEquals(profile, createResult);
    var deleteResult = profileDao.deleteBillingProfileById(profile.id());
    assertTrue(deleteResult);
    assertThrows(
        ProfileNotFoundException.class, () -> profileDao.getBillingProfileById(profile.id()));
  }

  @Test
  void listProfiles() {
    var profiles =
        Stream.generate(() -> profileDao.createBillingProfile(makeGCPProfile(), user))
            .limit(10)
            .collect(Collectors.toList());
    var keys = profiles.stream().map(BillingProfile::id).collect(Collectors.toList());
    var listResult = profileDao.listBillingProfiles(0, 10, keys);
    assertProfileListEquals(profiles, listResult);
  }

  @Test
  void listProfiles_subset() {
    var profiles =
        Stream.generate(() -> profileDao.createBillingProfile(makeGCPProfile(), user))
            .limit(10)
            .collect(Collectors.toList());
    var keys = profiles.stream().limit(3).map(BillingProfile::id).collect(Collectors.toList());
    var listResult = profileDao.listBillingProfiles(0, 10, keys);
    assertProfileListEquals(profiles.stream().limit(3).collect(Collectors.toList()), listResult);
  }

  @Test
  void listProfiles_offset() {
    var profiles =
        Stream.generate(() -> profileDao.createBillingProfile(makeGCPProfile(), user))
            .limit(10)
            .collect(Collectors.toList());
    var keys = profiles.stream().map(BillingProfile::id).collect(Collectors.toList());
    var listResult = profileDao.listBillingProfiles(5, 10, keys);
    assertProfileListEquals(profiles.stream().skip(5).collect(Collectors.toList()), listResult);
  }

  @Test
  void listProfiles_limit() {
    var profiles =
        Stream.generate(() -> profileDao.createBillingProfile(makeGCPProfile(), user))
            .limit(10)
            .collect(Collectors.toList());
    var keys = profiles.stream().map(BillingProfile::id).collect(Collectors.toList());
    var listResult = profileDao.listBillingProfiles(0, 5, keys);
    assertProfileListEquals(profiles.stream().limit(5).collect(Collectors.toList()), listResult);
  }

  @Test
  void listManagedResourceGroupsInSubscription() {
    UUID tenantId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();
    String managedResourceGroupId = "managedResourceGroupId";
    String differentSubscriptionMRGId = "differentSubscriptionMRGId";

    var profile = makeAzureProfile(tenantId, subscriptionId, managedResourceGroupId);
    profileDao.createBillingProfile(profile, user);

    var differentSubscriptionProfile =
        makeAzureProfile(tenantId, UUID.randomUUID(), differentSubscriptionMRGId);
    profileDao.createBillingProfile(differentSubscriptionProfile, user);

    var result = profileDao.listManagedResourceGroupsInSubscription(subscriptionId);
    assertEquals(List.of(managedResourceGroupId), result);
  }

  @Test
  void updateProfile_descriptionOnly() {
    var profile = makeGCPProfile();
    profileDao.createBillingProfile(profile, user);

    assert profileDao.updateProfile(profile.id(), "new description", null);
    var result = profileDao.getBillingProfileById(profile.id());
    assertEquals("new description", result.description());
    assertEquals(profile.billingAccountId(), result.billingAccountId());
  }

  @Test
  void updateProfile_billingAccountOnly() {
    var profile = makeGCPProfile();
    profileDao.createBillingProfile(profile, user);

    assert profileDao.updateProfile(profile.id(), null, "newBillingAccountId");
    var result = profileDao.getBillingProfileById(profile.id());
    assertEquals(profile.description(), result.description());
    assertEquals(Optional.of("newBillingAccountId"), result.billingAccountId());
  }

  @Test
  void updateProfile_descriptionAndBillingAccount() {
    var profile = makeGCPProfile();
    profileDao.createBillingProfile(profile, user);

    assert profileDao.updateProfile(profile.id(), "new description", "newBillingAccountId");
    var result = profileDao.getBillingProfileById(profile.id());
    assertEquals("new description", result.description());
    assertEquals(Optional.of("newBillingAccountId"), result.billingAccountId());
  }

  @Test
  void updateProfile_throwNoFields() {
    var profile = makeGCPProfile();
    profileDao.createBillingProfile(profile, user);

    assertThrows(
        MissingRequiredFieldsException.class,
        () -> profileDao.updateProfile(profile.id(), null, null));
  }

  @Test
  void updateProfile_falseIfProfileNotFound() {
    UUID notFoundId = UUID.randomUUID();
    assertThrows(
        ProfileNotFoundException.class, () -> profileDao.getBillingProfileById(notFoundId));

    assert !profileDao.updateProfile(notFoundId, "description", "billingAccount");
  }

  @Test
  void removeBillingAccount() {
    var profile = makeGCPProfile();
    profileDao.createBillingProfile(profile, user);
    assert profileDao.removeBillingAccount(profile.id());
    assertEquals(
        Optional.empty(), profileDao.getBillingProfileById(profile.id()).billingAccountId());
  }

  // Keeps track of the profiles that are made so they can be cleaned up
  private BillingProfile makeGCPProfile() {
    var uuid = UUID.randomUUID();
    profileIds.add(uuid);
    return new BillingProfile(
        uuid,
        "testProfile",
        "Test Profile",
        "direct",
        CloudPlatform.GCP,
        Optional.of("billingAccount"),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        null,
        null,
        null);
  }

  private BillingProfile makeAzureProfile(
      UUID tenantId, UUID subscriptionId, String managedResourceGroupId) {
    UUID profileId = UUID.randomUUID();
    profileIds.add(profileId);
    return new BillingProfile(
        profileId,
        "test profile",
        "",
        "direct",
        CloudPlatform.AZURE,
        Optional.empty(),
        Optional.of(tenantId),
        Optional.of(subscriptionId),
        Optional.of(managedResourceGroupId),
        null,
        null,
        null);
  }

  // Tests profile equality, ignoring createdTime and createdBy
  private void assertProfileEquals(BillingProfile expected, BillingProfile actual) {
    assertEquals(expected.id(), actual.id());
    assertEquals(expected.displayName(), actual.displayName());
    assertEquals(expected.description(), actual.description());
    assertEquals(expected.biller(), actual.biller());
    assertEquals(expected.cloudPlatform(), actual.cloudPlatform());
    assertEquals(expected.billingAccountId(), actual.billingAccountId());
    assertEquals(expected.tenantId(), actual.tenantId());
    assertEquals(expected.subscriptionId(), actual.subscriptionId());
    assertEquals(expected.managedResourceGroupId(), actual.managedResourceGroupId());
    assertNotNull(actual.createdTime());
    assertNotNull(actual.lastModified());
    assertEquals(user.getEmail(), actual.createdBy());
  }

  private void assertProfileListEquals(List<BillingProfile> expected, List<BillingProfile> actual) {
    var expectedMap = expected.stream().collect(Collectors.toMap(BillingProfile::id, identity()));
    var actualMap = actual.stream().collect(Collectors.toMap(BillingProfile::id, identity()));
    assertEquals(expectedMap.keySet(), actualMap.keySet());
    actualMap.forEach((k, v) -> assertProfileEquals(expectedMap.get(k), v));
  }
}
