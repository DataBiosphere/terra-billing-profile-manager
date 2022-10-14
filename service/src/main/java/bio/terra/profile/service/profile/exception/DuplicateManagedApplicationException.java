package bio.terra.profile.service.profile.exception;

import bio.terra.common.exception.ConflictException;

public class DuplicateManagedApplicationException extends ConflictException {
  public DuplicateManagedApplicationException(String message) {
    super(message);
  }

  public DuplicateManagedApplicationException(String message, Throwable cause) {
    super(message, cause);
  }

  public DuplicateManagedApplicationException(Throwable cause) {
    super(cause);
  }
}
