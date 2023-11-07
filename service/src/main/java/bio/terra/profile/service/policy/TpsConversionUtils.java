package bio.terra.profile.service.policy;

import bio.terra.policy.model.TpsPolicyInput;
import bio.terra.policy.model.TpsPolicyInputs;
import bio.terra.policy.model.TpsPolicyPair;
import bio.terra.profile.model.BpmApiPolicyInput;
import bio.terra.profile.model.BpmApiPolicyInputs;
import bio.terra.profile.model.BpmApiPolicyPair;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class TpsConversionUtils {

  public static TpsPolicyInput tpsFromBpmApiPolicyInput(BpmApiPolicyInput apiInput) {
    List<TpsPolicyPair> additionalData = new ArrayList<>();
    for (BpmApiPolicyPair apiPair : apiInput.getAdditionalData()) {
      additionalData.add(new TpsPolicyPair().key(apiPair.getKey()).value(apiPair.getValue()));
    }

    return new TpsPolicyInput()
        .namespace(apiInput.getNamespace())
        .name(apiInput.getName())
        .additionalData(additionalData);
  }

  public static @Nullable TpsPolicyInputs tpsFromBpmApiPolicyInputs(
      @Nullable BpmApiPolicyInputs apiInputs) {
    if (apiInputs == null) {
      return null;
    }
    return new TpsPolicyInputs()
        .inputs(
            apiInputs.getInputs().stream()
                .map(TpsConversionUtils::tpsFromBpmApiPolicyInput)
                .toList());
  }

  public static BpmApiPolicyInput bpmFromTpsPolicyInput(TpsPolicyInput tpsInput) {
    List<BpmApiPolicyPair> additionalData = new ArrayList<>();
    for (TpsPolicyPair tpsPair : tpsInput.getAdditionalData()) {
      additionalData.add(new BpmApiPolicyPair().key(tpsPair.getKey()).value(tpsPair.getValue()));
    }

    return new BpmApiPolicyInput()
        .namespace(tpsInput.getNamespace())
        .name(tpsInput.getName())
        .additionalData(additionalData);
  }

  public static BpmApiPolicyInputs bpmFromTpsPolicyInputs(TpsPolicyInputs tpsInputs) {
    if (tpsInputs == null) {
      return null;
    }

    return new BpmApiPolicyInputs()
        .inputs(
            tpsInputs.getInputs().stream().map(TpsConversionUtils::bpmFromTpsPolicyInput).toList());
  }
}
