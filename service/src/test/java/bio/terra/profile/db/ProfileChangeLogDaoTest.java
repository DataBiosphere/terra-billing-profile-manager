package bio.terra.profile.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.model.ChangeLogEntry;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.profile.model.BillingProfile;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ProfileChangeLogDaoTest extends BaseSpringUnitTest {

  @Autowired ProfileChangeLogDao dao;

  @Test
  void changeLogCreatesEntryForCreate() {
    var userId = UUID.randomUUID().toString();
    // specifying a distinct time to ensure it's using the time the profile was created,
    // not the time the record was created
    var profileCreateTime = Instant.now().minus(10, ChronoUnit.SECONDS);
    var profile =
        new BillingProfile(
            UUID.randomUUID(),
            "profile name",
            "profile description",
            "direct",
            CloudPlatform.AZURE,
            Optional.empty(),
            Optional.of(UUID.randomUUID()),
            Optional.of(UUID.randomUUID()),
            Optional.of("mrg_id"),
            profileCreateTime,
            profileCreateTime,
            userId);

    var recordId = dao.recordProfileCreate(profile, userId);
    var records = dao.getChangesByProfile(profile.id());

    assertEquals(1, records.size());
    var record = records.get(0);
    assertEquals(recordId.get(), record.getId());
    assertEquals(profile.id(), record.getProfileId());
    assertEquals(ChangeLogEntry.ChangeTypeEnum.CREATE, record.getChangeType());
    assertEquals(userId, record.getChangeBy());
    assertEquals(Date.from(profileCreateTime), record.getChangeDate());
  }

  @Test
  void changeLogCreatesUpdateEntryWithChanges() {
    var descriptionChange = Map.of("oldValue", "old description", "newValue", "new description");
    var profileId = UUID.randomUUID();
    var userId = UUID.randomUUID().toString();
    var changes = Map.of("description", descriptionChange);

    var recordId = dao.recordProfileUpdate(profileId, userId, changes);
    var records = dao.getChangesByProfile(profileId);

    assertEquals(1, records.size());
    var record = records.get(0);
    assertEquals(recordId.get(), record.getId());
    assertEquals(profileId, record.getProfileId());
    assertEquals(userId, record.getChangeBy());
    assertEquals(ChangeLogEntry.ChangeTypeEnum.UPDATE, record.getChangeType());
    assertEquals(changes, record.getChanges());
  }

  @Test
  void changeLogCreatesEntryForDelete() {
    var profileId = UUID.randomUUID();
    var userId = UUID.randomUUID().toString();

    var recordId = dao.recordProfileDelete(profileId, userId);
    var records = dao.getChangesByProfile(profileId);

    assertEquals(1, records.size());
    var record = records.get(0);
    assertEquals(recordId.get(), record.getId());
    assertEquals(profileId, record.getProfileId());
    assertEquals(ChangeLogEntry.ChangeTypeEnum.DELETE, record.getChangeType());
    assertEquals(userId, record.getChangeBy());
  }

  @Test
  void changeLogOrdersUpdatesByDate() {
    var userId = UUID.randomUUID().toString();
    var profileCreateTime = Instant.now().minus(10, ChronoUnit.SECONDS);
    var profile =
        new BillingProfile(
            UUID.randomUUID(),
            "profile name",
            "profile description",
            "direct",
            CloudPlatform.AZURE,
            Optional.empty(),
            Optional.of(UUID.randomUUID()),
            Optional.of(UUID.randomUUID()),
            Optional.of("mrg_id"),
            profileCreateTime,
            profileCreateTime,
            userId);
    var descriptionChange = Map.of("oldValue", "old description", "newValue", "new description");
    var changes = Map.of("description", descriptionChange);

    var updateRecordId = dao.recordProfileUpdate(profile.id(), userId, changes);
    var createRecordId = dao.recordProfileCreate(profile, userId);

    var records = dao.getChangesByProfile(profile.id());
    assertEquals(2, records.size());
    assertEquals(createRecordId.get(), records.get(0).getId());
    assertEquals(updateRecordId.get(), records.get(1).getId());
  }
}
