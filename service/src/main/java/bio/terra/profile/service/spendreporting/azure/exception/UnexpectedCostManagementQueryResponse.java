package bio.terra.profile.service.spendreporting.azure.exception;

import bio.terra.common.exception.InternalServerErrorException;

public class UnexpectedCostManagementQueryResponse extends InternalServerErrorException {
  public UnexpectedCostManagementQueryResponse(String message) {
    super(message);
  }
}
