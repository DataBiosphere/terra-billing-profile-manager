package bio.terra.profile.service.spendreporting;

import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.model.SpendReport;
import bio.terra.profile.service.iam.SamRethrow;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.iam.model.SamAction;
import bio.terra.profile.service.iam.model.SamResourceType;
import bio.terra.profile.service.profile.ProfileService;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.spendreporting.azure.AzureSpendReportingService;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import bio.terra.profile.service.spendreporting.azure.model.mapper.SpendDataMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpendReportingService {
  private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

  private final ProfileDao profileDao;
  private final SamService samService;
  private final AzureSpendReportingService azureSpendReportingService;
  private final SpendDataMapper spendDataMapper;

  @Autowired
  public SpendReportingService(
      ProfileDao profileDao,
      SamService samService,
      AzureSpendReportingService azureSpendReportingService,
      SpendDataMapper spendDataMapper) {
    this.profileDao = profileDao;
    this.samService = samService;
    this.azureSpendReportingService = azureSpendReportingService;
    this.spendDataMapper = spendDataMapper;
  }

  public SpendReport getSpendReport(
      UUID id,
      OffsetDateTime startDate,
      OffsetDateTime endDate,
      AuthenticatedUserRequest userRequest) {
    SamRethrow.onInterrupted(
        () ->
            samService.verifyAuthorization(
                userRequest, SamResourceType.PROFILE, id, SamAction.READ_SPEND_REPORT),
        "verifyAuthorization");
    BillingProfile profile = profileDao.getBillingProfileById(id);
    if (profile.cloudPlatform().equals(CloudPlatform.AZURE)) {
      SpendData spendData =
          azureSpendReportingService.getBillingProjectSpendData(profile, startDate, endDate);
      return spendDataMapper.mapSpendData(spendData);
    } else {
      throw new NotImplementedException(
          String.format(
              "Spend reporting for %s billing profiles is unsupported.", profile.cloudPlatform()));
    }
  }
}
