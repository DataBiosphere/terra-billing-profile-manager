package bio.terra.profile.service.azure;

import bio.terra.common.exception.InternalServerErrorException;

public class InvalidAzureResourceGroupNameException extends InternalServerErrorException {
  public InvalidAzureResourceGroupNameException(String message) {
    super(message);
  }
}
