package bio.terra.profile.service.profile.flight.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.profile.common.BaseSpringUnitTest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class UpdateProfileRecordStepTest extends BaseSpringUnitTest {

  @Mock private FlightContext flightContext;
  @Mock private ProfileDao profileDao;

  private BillingProfile profile;
  private UpdateProfileRecordStep step;
  private final String newDescription = "newDescription";
  private final String newBillingAccount = "newBillingAccount";

  @BeforeEach
  public void before() {
    profile =
        new BillingProfile(
            UUID.randomUUID(),
            "name",
            "description",
            "direct",
            CloudPlatform.GCP,
            Optional.of("billingAccount"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Instant.now(),
            Instant.now(),
            "creator");

    step = new UpdateProfileRecordStep(profileDao, profile, newDescription, newBillingAccount);
  }

  @Test
  void doStepSuccess() {
    when(profileDao.updateProfile(profile.id(), newDescription, newBillingAccount))
        .thenReturn(true);
    var result = step.doStep(flightContext);
    assertEquals(StepResult.getStepResultSuccess(), result);
    verify(profileDao).updateProfile(profile.id(), newDescription, newBillingAccount);
  }

  @Test
  void doStepFailure() {
    when(profileDao.updateProfile(profile.id(), newDescription, newBillingAccount))
        .thenReturn(false);
    var result = step.doStep(flightContext);
    assertEquals(StepStatus.STEP_RESULT_FAILURE_FATAL, result.getStepStatus());
    verify(profileDao).updateProfile(profile.id(), newDescription, newBillingAccount);
  }

  @Test
  void undoStep() {
    when(profileDao.updateProfile(
            profile.id(), profile.description(), profile.getRequiredBillingAccountId()))
        .thenReturn(true);
    var result = step.undoStep(flightContext);
    assertEquals(StepResult.getStepResultSuccess(), result);
    verify(profileDao, times(0)).updateProfile(profile.id(), newDescription, newBillingAccount);
    verify(profileDao)
        .updateProfile(profile.id(), profile.description(), profile.getRequiredBillingAccountId());
  }
}
