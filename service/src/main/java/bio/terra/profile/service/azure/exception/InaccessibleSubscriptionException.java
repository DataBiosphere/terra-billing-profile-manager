package bio.terra.profile.service.azure.exception;

import bio.terra.common.exception.NotFoundException;

public class InaccessibleSubscriptionException extends NotFoundException {
  public InaccessibleSubscriptionException(String message, Throwable cause) {
    super(message, cause);
  }
}
