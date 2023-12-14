package bio.terra.profile.service.profile.exception;

import bio.terra.common.exception.BadRequestException;

public class InvalidFieldException extends BadRequestException {
  public InvalidFieldException(String message) {
    super(message);
  }
}
