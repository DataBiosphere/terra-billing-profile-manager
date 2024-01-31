package bio.terra.profile.service.spendreporting.azure.exception;

import bio.terra.common.exception.InternalServerErrorException;

public class UnexpectedCostManagementDataFormat extends InternalServerErrorException {
  public UnexpectedCostManagementDataFormat(String message) {
    super(message);
  }
}
