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
import bio.terra.profile.service.spendreporting.azure.AzureCostManagementQuery;
import java.util.Date;
import java.util.List;
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
  private final AzureCostManagementQuery azureCostManagementQuery;

  @Autowired
  public SpendReportingService(
      ProfileDao profileDao,
      SamService samService,
      AzureCostManagementQuery azureCostManagementQuery) {
    this.profileDao = profileDao;
    this.samService = samService;
    this.azureCostManagementQuery = azureCostManagementQuery;
  }

  public SpendReport getSpendReport(
      UUID id,
      Date startDate,
      Date endDate,
      List<String> aggregationKeys,
      AuthenticatedUserRequest userRequest) {
    SamRethrow.onInterrupted(
        () ->
            samService.isAuthorized(
                userRequest, SamResourceType.PROFILE, id, SamAction.READ_SPEND_REPORT),
        "checkSpendReportAuthz");
    BillingProfile profile = profileDao.getBillingProfileById(id);
    if (profile.cloudPlatform().equals(CloudPlatform.AZURE)) {
      // fetch spend data from Azure
      // turn data into SpendReport
    } else {
      throw new NotImplementedException(
          String.format(
              "Spend reporting for %s billing profiles is unsupported.", profile.cloudPlatform()));
    }

    return new SpendReport();
  }
}
