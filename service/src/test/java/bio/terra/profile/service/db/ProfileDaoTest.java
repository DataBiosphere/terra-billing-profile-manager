package bio.terra.profile.service.db;

import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
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

public class ProfileDaoTest extends BaseUnitTest {
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
  public void createAndGetProfile() {
    var profile = makeProfile();
    var createResult = profileDao.createBillingProfile(profile, user);
    assertProfileEquals(profile, createResult);
    var getResult = profileDao.getBillingProfileById(profile.id());
    assertProfileEquals(profile, getResult);
  }

  @Test
  public void createProfile_alreadyExists() {
    var profile = makeProfile();
    var createResult = profileDao.createBillingProfile(profile, user);
    assertProfileEquals(profile, createResult);
    assertThrows(DuplicateKeyException.class, () -> profileDao.createBillingProfile(profile, user));
  }

  @Test
  public void createAndDeleteProfile() {
    var profile = makeProfile();
    var createResult = profileDao.createBillingProfile(profile, user);
    assertProfileEquals(profile, createResult);
    var deleteResult = profileDao.deleteBillingProfileById(profile.id());
    assertTrue(deleteResult);
    assertThrows(
        ProfileNotFoundException.class, () -> profileDao.getBillingProfileById(profile.id()));
  }

  @Test
  public void listProfiles() {
    var profiles =
        Stream.generate(() -> profileDao.createBillingProfile(makeProfile(), user))
            .limit(10)
            .collect(Collectors.toList());
    var keys = profiles.stream().map(BillingProfile::id).collect(Collectors.toList());
    var listResult = profileDao.listBillingProfiles(0, 10, keys);
    assertProfileListEquals(profiles, listResult);
  }

  @Test
  public void listProfiles_subset() {
    var profiles =
        Stream.generate(() -> profileDao.createBillingProfile(makeProfile(), user))
            .limit(10)
            .collect(Collectors.toList());
    var keys = profiles.stream().limit(3).map(BillingProfile::id).collect(Collectors.toList());
    var listResult = profileDao.listBillingProfiles(0, 10, keys);
    assertProfileListEquals(profiles.stream().limit(3).collect(Collectors.toList()), listResult);
  }

  @Test
  public void listProfiles_offset() {
    var profiles =
        Stream.generate(() -> profileDao.createBillingProfile(makeProfile(), user))
            .limit(10)
            .collect(Collectors.toList());
    var keys = profiles.stream().map(BillingProfile::id).collect(Collectors.toList());
    var listResult = profileDao.listBillingProfiles(5, 10, keys);
    assertProfileListEquals(profiles.stream().skip(5).collect(Collectors.toList()), listResult);
  }

  @Test
  public void listProfiles_limit() {
    var profiles =
        Stream.generate(() -> profileDao.createBillingProfile(makeProfile(), user))
            .limit(10)
            .collect(Collectors.toList());
    var keys = profiles.stream().map(BillingProfile::id).collect(Collectors.toList());
    var listResult = profileDao.listBillingProfiles(0, 5, keys);
    assertProfileListEquals(profiles.stream().limit(5).collect(Collectors.toList()), listResult);
  }

  // Keeps track of the profiles that are made so they can be cleaned up
  private BillingProfile makeProfile() {
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
        Optional.empty(),
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
    assertEquals(expected.resourceGroupName(), actual.resourceGroupName());
    assertEquals(expected.applicationDeploymentName(), actual.applicationDeploymentName());
    assertNotNull(actual.createdTime());
    assertEquals(user.getEmail(), actual.createdBy());
  }

  private void assertProfileListEquals(List<BillingProfile> expected, List<BillingProfile> actual) {
    var expectedMap = expected.stream().collect(Collectors.toMap(BillingProfile::id, identity()));
    var actualMap = actual.stream().collect(Collectors.toMap(BillingProfile::id, identity()));
    assertEquals(expectedMap.keySet(), actualMap.keySet());
    actualMap.forEach((k, v) -> assertProfileEquals(expectedMap.get(k), v));
  }
}
