package bio.terra.profile.service.policy;

import bio.terra.policy.model.TpsPolicyInput;
import bio.terra.policy.model.TpsPolicyInputs;
import bio.terra.policy.model.TpsPolicyPair;
import bio.terra.profile.model.BPMPolicyInput;
import bio.terra.profile.model.BPMPolicyInputs;
import bio.terra.profile.model.BPMPolicyPair;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class TpsConversionUtils {

  public static TpsPolicyInput tpsFromApiPolicyInput(BPMPolicyInput apiInput) {
    List<TpsPolicyPair> additionalData = new ArrayList<>();
    for (BPMPolicyPair apiPair : apiInput.getAdditionalData()) {
      additionalData.add(new TpsPolicyPair().key(apiPair.getKey()).value(apiPair.getValue()));
    }

    return new TpsPolicyInput()
        .namespace(apiInput.getNamespace())
        .name(apiInput.getName())
        .additionalData(additionalData);
  }

  public static @Nullable TpsPolicyInputs tpsFromApiTpsPolicyInputs(
      @Nullable BPMPolicyInputs apiInputs) {
    if (apiInputs == null) {
      return null;
    }
    return new TpsPolicyInputs()
        .inputs(
            apiInputs.getInputs().stream().map(TpsConversionUtils::tpsFromApiPolicyInput).toList());
  }
}
