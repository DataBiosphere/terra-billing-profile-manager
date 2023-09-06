package bio.terra.profile.service.policy;

import bio.terra.policy.model.TpsComponent;
import bio.terra.policy.model.TpsObjectType;
import bio.terra.policy.model.TpsPolicyInput;
import bio.terra.policy.model.TpsPolicyInputs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TpsService {

  TpsApiDispatch apiDispatch;

  @Autowired
  TpsService(TpsApiDispatch apiDispatch) {
    this.apiDispatch = apiDispatch;
  }

  public void attachProtectedDataPolicy(UUID billingProjectId) throws InterruptedException {
    TpsPolicyInputs inputs = new TpsPolicyInputs();
    inputs.addInputsItem(new TpsPolicyInput().name("protected-data").namespace("terra"));
    apiDispatch.createPao(billingProjectId, inputs, TpsComponent.BPM, TpsObjectType.BILLING_PROFILE);
  }

}
