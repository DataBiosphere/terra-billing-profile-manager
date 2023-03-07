package bio.terra.profile.service.spendreporting.azure.exception;

import bio.terra.common.exception.NotFoundException;

public class KubernetesResourceNotFound extends NotFoundException {
  public KubernetesResourceNotFound(String message) {
    super(message);
  }
}
