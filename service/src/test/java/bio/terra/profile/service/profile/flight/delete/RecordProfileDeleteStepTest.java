package bio.terra.profile.service.profile.flight.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.db.ProfileChangeLogDao;
import bio.terra.stairway.StepResult;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class RecordProfileDeleteStepTest extends BaseUnitTest {

  @Test
  void recordsTheBillingProfileDeletionInTheChangelog() throws Exception {
    var userId = UUID.randomUUID().toString();
    var userRequest = mock(AuthenticatedUserRequest.class);
    when(userRequest.getSubjectId()).thenReturn(userId);
    var profileId = UUID.randomUUID();
    var changeLogDao = mock(ProfileChangeLogDao.class);
    when(changeLogDao.recordProfileDelete(profileId, userId))
        .thenReturn(Optional.of(UUID.randomUUID()));
    var step = new RecordProfileDeleteStep(changeLogDao, profileId, userRequest);
    var result = step.doStep(mock());

    assertEquals(StepResult.getStepResultSuccess(), result);
    verify(changeLogDao).recordProfileDelete(profileId, userId);
  }
}
