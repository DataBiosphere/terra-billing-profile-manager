package bio.terra.profile.service.spendreporting.azure.exception;

import bio.terra.common.exception.InternalServerErrorException;

public class KubernetesResourceNotFound extends InternalServerErrorException {
  public KubernetesResourceNotFound(String message) {
    super(message);
  }
}
