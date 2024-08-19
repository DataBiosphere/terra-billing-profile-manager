package bio.terra.profile.service.profile.model;

import bio.terra.profile.model.ChangeLogModel;
import bio.terra.profile.model.ChangeType;
import java.util.Date;
import java.util.UUID;

public record ChangeLogEntry(
    UUID id,
    UUID profileId,
    ChangeType changeType,
    String changeBy,
    Date changeDate,
    Object changes) {

  public ChangeLogModel toApiModel() {
    return new ChangeLogModel()
        .id(id)
        .profileId(profileId)
        .changeType(changeType)
        .changeBy(changeBy)
        .changeDate(changeDate)
        .changes(changes);
  }
}
