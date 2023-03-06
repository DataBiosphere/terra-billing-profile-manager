package bio.terra.profile.service.spendreporting.azure.exception;

import bio.terra.common.exception.InternalServerErrorException;

public class MultipleKubernetesResourcesFound extends InternalServerErrorException {
  public MultipleKubernetesResourcesFound(String message) {
    super(message);
  }
}
