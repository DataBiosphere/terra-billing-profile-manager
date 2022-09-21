package bio.terra.profile.service.profile.exception;

import bio.terra.common.exception.ConflictException;

public class DuplicateTagException extends ConflictException {
  public DuplicateTagException(String message) {
    super(message);
  }

  public DuplicateTagException(String message, Throwable cause) {
    super(message, cause);
  }

  public DuplicateTagException(Throwable cause) {
    super(cause);
  }
}
