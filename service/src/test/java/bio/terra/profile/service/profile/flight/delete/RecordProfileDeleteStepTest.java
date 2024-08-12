package bio.terra.profile.service.profile.flight.delete;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.db.ProfileChangeLogDao;
import bio.terra.stairway.StepResult;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class RecordProfileDeleteStepTest extends BaseUnitTest {

  @Test
  void recordsTheBillingProfileDeletionInTheChangelog() throws Exception {
    var userEmail = "some_user_email";
    var userRequest = mock(AuthenticatedUserRequest.class);
    when(userRequest.getEmail()).thenReturn(userEmail);
    var profileId = UUID.randomUUID();
    var changeLogDao = mock(ProfileChangeLogDao.class);
    when(changeLogDao.recordProfileDelete(profileId, userEmail)).thenReturn(Optional.of(UUID.randomUUID()));
    var step = new RecordProfileDeleteStep(changeLogDao, profileId, userRequest);
    var result = step.doStep(mock());

    assertEquals(StepResult.getStepResultSuccess(), result);
    verify(changeLogDao).recordProfileDelete(profileId, userEmail);
  }

}
