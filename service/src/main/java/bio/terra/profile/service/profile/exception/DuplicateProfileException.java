package bio.terra.profile.service.profile.exception;

import bio.terra.common.exception.ConflictException;

public class DuplicateProfileException extends ConflictException {
  public DuplicateProfileException(String message) {
    super(message);
  }

  public DuplicateProfileException(String message, Throwable cause) {
    super(message, cause);
  }

  public DuplicateProfileException(Throwable cause) {
    super(cause);
  }
}
