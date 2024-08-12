package bio.terra.profile.service.profile.flight.create;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.db.ProfileChangeLogDao;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import bio.terra.profile.service.profile.flight.ProfileMapKeys;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.StepResult;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class RecordProfileCreationStepTest extends BaseUnitTest {


  @Test
  void failsWhenNoBillingProfileInWorkingMap() {
    var workingMap = mock(FlightMap.class);
    var context = mock(FlightContext.class);
    when(context.getWorkingMap()).thenReturn(workingMap);
    var changeLogDao = mock(ProfileChangeLogDao.class);
    var step = new RecordProfileCreationStep(changeLogDao);
    assertThrows(
        MissingRequiredFieldsException.class,
        () -> step.doStep(context)
    );
  }

  @Test
  void recordsTheBillingProfileCreationInTheChangelog() throws Exception {
    var profile = mock( BillingProfile.class);
    var workingMap = mock(FlightMap.class);
    when(workingMap.get(ProfileMapKeys.PROFILE, BillingProfile.class)).thenReturn(profile);
    var context = mock(FlightContext.class);
    when(context.getWorkingMap()).thenReturn(workingMap);

    var changeLogDao = mock(ProfileChangeLogDao.class);

    when(changeLogDao.recordProfileCreate(profile)).thenReturn(Optional.of(UUID.randomUUID()));

    var step = new RecordProfileCreationStep(changeLogDao);
    var result = step.doStep(context);
    assertEquals(StepResult.getStepResultSuccess(), result);
    verify(changeLogDao).recordProfileCreate(profile);
  }
}
