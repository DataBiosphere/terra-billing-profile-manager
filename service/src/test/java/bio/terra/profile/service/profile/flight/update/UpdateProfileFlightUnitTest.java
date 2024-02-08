package bio.terra.profile.service.profile.flight.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.model.UpdateProfileRequest;
import bio.terra.profile.service.job.JobMapKeys;
import bio.terra.profile.service.profile.flight.common.VerifyUserBillingAccountAccessStep;
import bio.terra.stairway.FlightMap;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

class UpdateProfileFlightUnitTest extends BaseUnitTest {

  @Test
  void updateProfileDescriptionSteps() {
    var context = mock(ApplicationContext.class);
    var request = new UpdateProfileRequest().description("description");
    var inputParameters = new FlightMap();
    inputParameters.put(JobMapKeys.REQUEST.getKeyName(), request);

    var flight = new UpdateProfileFlight(inputParameters, context);
    var steps = flight.getSteps();
    assertEquals(3, steps.size());
    assertEquals(VerifyProfileMetadataUpdateAuthorizationStep.class, steps.get(0).getClass());
    assertEquals(UpdateProfileRecordStep.class, steps.get(1).getClass());
    assertEquals(UpdateProfileSetResponseStep.class, steps.get(2).getClass());
  }

  @Test
  void updateProfileBillingAccountSteps() {
    var context = mock(ApplicationContext.class);
    var request = new UpdateProfileRequest().billingAccountId("billingAccount");
    var inputParameters = new FlightMap();
    inputParameters.put(JobMapKeys.REQUEST.getKeyName(), request);

    var flight = new UpdateProfileFlight(inputParameters, context);
    var steps = flight.getSteps();
    assertEquals(4, steps.size());
    assertEquals(VerifyAccountUpdateAuthorizationStep.class, steps.get(0).getClass());
    assertEquals(VerifyUserBillingAccountAccessStep.class, steps.get(1).getClass());
    assertEquals(UpdateProfileRecordStep.class, steps.get(2).getClass());
    assertEquals(UpdateProfileSetResponseStep.class, steps.get(3).getClass());
  }

  @Test
  void updateProfileDescriptionAndBillingAccountSteps() {
    var context = mock(ApplicationContext.class);
    var request =
        new UpdateProfileRequest().description("description").billingAccountId("billingAccount");

    var inputParameters = new FlightMap();
    inputParameters.put(JobMapKeys.REQUEST.getKeyName(), request);

    var flight = new UpdateProfileFlight(inputParameters, context);
    var steps = flight.getSteps();
    assertEquals(5, steps.size());
    assertEquals(VerifyProfileMetadataUpdateAuthorizationStep.class, steps.get(0).getClass());
    assertEquals(VerifyAccountUpdateAuthorizationStep.class, steps.get(1).getClass());
    assertEquals(VerifyUserBillingAccountAccessStep.class, steps.get(2).getClass());
    assertEquals(UpdateProfileRecordStep.class, steps.get(3).getClass());
    assertEquals(UpdateProfileSetResponseStep.class, steps.get(4).getClass());
  }
}
