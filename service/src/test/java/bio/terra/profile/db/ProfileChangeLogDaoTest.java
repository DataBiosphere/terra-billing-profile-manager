package bio.terra.profile.db;

import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.model.ChangeLogEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ProfileChangeLogDaoTest extends BaseSpringUnitTest {

  @Autowired ProfileChangeLogDao dao;

  @Test
  void changeLogCreatesEntryForDelete() {
    var profileId = UUID.randomUUID();
    var user = "user@test.com";
    var recordId = dao.recordProfileDelete(profileId, user);
    var records = dao.getChangesByProfile(profileId);
    assertEquals(1, records.size());
    var record = records.get(0);
    assertEquals(recordId.get(), record.getId());
    assertEquals(profileId, record.getProfileId());
    assertEquals(ChangeLogEntry.ChangeTypeEnum.DELETE, record.getChangeType());
    assertEquals(user, record.getChangeBy());
  }

  @Test
  void changeLogCreatesUpdateEntryWithChanges() {
    var descriptionChange = Map.of("oldValue", "old description", "newValue", "new description");
    var profileId = UUID.randomUUID();
    var user = "user@test.com";
    var changes = Map.of("description", descriptionChange);

    var recordId = dao.recordProfileUpdate(profileId, user, changes);

    var records = dao.getChangesByProfile(profileId);
    assertEquals(1, records.size());
    var record = records.get(0);
    assertEquals(recordId.get(), record.getId());
    assertEquals(profileId, record.getProfileId());
    assertEquals(user, record.getChangeBy());
    assertEquals(ChangeLogEntry.ChangeTypeEnum.UPDATE, record.getChangeType());
    assertEquals(changes, record.getChanges());
  }

}
