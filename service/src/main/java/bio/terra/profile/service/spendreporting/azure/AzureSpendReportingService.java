package bio.terra.profile.service.spendreporting.azure;

import bio.terra.profile.app.configuration.CacheConfiguration;
import bio.terra.profile.service.crl.AzureCrlService;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.spendreporting.azure.exception.KubernetesResourceNotFound;
import bio.terra.profile.service.spendreporting.azure.exception.MultipleKubernetesResourcesFound;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import bio.terra.profile.service.spendreporting.azure.model.mapper.QueryResultMapper;
import com.azure.core.http.rest.Response;
import com.azure.resourcemanager.containerservice.ContainerServiceManager;
import com.azure.resourcemanager.costmanagement.models.QueryResult;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.fluentcore.arm.models.HasName;
import com.azure.resourcemanager.resources.models.GenericResource;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AzureSpendReportingService {
  private static final Logger logger = LoggerFactory.getLogger(AzureSpendReportingService.class);

  public static final String AZURE_KUBERNETES_RESOURCE_TYPE = "managedclusters";
  private final AzureCostManagementQuery azureCostManagementQuery;
  private final AzureCrlService crlService;
  private final QueryResultMapper queryResultMapper;
  private final CacheManager cacheManager;

  public AzureSpendReportingService(
      AzureCostManagementQuery azureCostManagementQuery,
      AzureCrlService crlService,
      QueryResultMapper queryResultMapper,
      CacheManager cacheManager) {
    this.azureCostManagementQuery = azureCostManagementQuery;
    this.crlService = crlService;
    this.queryResultMapper = queryResultMapper;
    this.cacheManager = cacheManager;
  }

  /**
   * Returns spend data for the specified period. Resources related to an Azure billing project
   * reside in the 2 resource groups: 1st - managed resource group related to the Terra managed
   * application 2nd - K8s node resource group.
   *
   * @param billingProfile The billing profile associated with the billing project.
   * @param from Start of the billing period
   * @param to End of the billing period
   * @return Spend data
   */
  @Cacheable(value = CacheConfiguration.AZURE_SPEND_REPORT_CACHE_NAME)
  public SpendData getBillingProfileSpendData(
      BillingProfile billingProfile, OffsetDateTime from, OffsetDateTime to) {
    String k8sNodeResourceGroup =
        getK8sNodeResourceGroupName(
            billingProfile.getRequiredTenantId(),
            billingProfile.getRequiredSubscriptionId(),
            billingProfile.getRequiredManagedResourceGroupId());

    List<CompletableFuture<SpendData>> azureCostManagementQueriesFutures =
        List.of(
            CompletableFuture.supplyAsync(
                () ->
                    querySpendDataForResourceGroup(
                        billingProfile.getRequiredSubscriptionId(),
                        billingProfile.getRequiredManagedResourceGroupId(),
                        from,
                        to,
                        false)),
            CompletableFuture.supplyAsync(
                () ->
                    querySpendDataForResourceGroup(
                        billingProfile.getRequiredSubscriptionId(),
                        k8sNodeResourceGroup,
                        from,
                        to,
                        true)));

    List<SpendData> spendDataList =
        azureCostManagementQueriesFutures.stream().map(CompletableFuture::join).toList();

    logger.info(
        "Azure spend report data for billing profile with Id=`{}` and period from=`{}` to=`{}` received.",
        billingProfile.id(),
        from,
        to);
    return new SpendData(
        spendDataList.stream().flatMap(sp -> sp.getSpendDataItems().stream()).toList(), from, to);
  }

  private SpendData querySpendDataForResourceGroup(
      UUID subscriptionId,
      String resourceGroupName,
      OffsetDateTime from,
      OffsetDateTime to,
      boolean isAksResourceGroup) {
    Response<QueryResult> costQueryResponse =
        azureCostManagementQuery.resourceGroupCostQueryWithResourceTypeGrouping(
            subscriptionId, resourceGroupName, from, to);
    if (isAksResourceGroup) {
      return queryResultMapper.mapQueryResult(
          costQueryResponse.getValue(), SpendCategoryType.COMPUTE, from, to);
    } else {
      return queryResultMapper.mapQueryResult(costQueryResponse.getValue(), from, to);
    }
  }

  /**
   * Returns the name of the corresponding K8s node resource group.
   *
   * @param resourceGroupName Name of the managed resource group
   * @return Name of the K8s node resource group
   */
  private String getK8sNodeResourceGroupName(
      UUID tenantId, UUID subscriptionId, String resourceGroupName) {
    ResourceManager resourceManager = crlService.getResourceManager(tenantId, subscriptionId);
    List<GenericResource> k8sResources =
        resourceManager.genericResources().listByResourceGroup(resourceGroupName).stream()
            .filter(
                gr ->
                    gr.resourceType()
                        .toLowerCase(Locale.ROOT)
                        .startsWith(AZURE_KUBERNETES_RESOURCE_TYPE))
            .toList();
    if (k8sResources.isEmpty()) {
      throw new KubernetesResourceNotFound(
          String.format(
              "Cannot find K8s resource in the managed resource group with name '%s'",
              resourceGroupName));
    }
    // we don't expect multiple K8s resources in the MRG
    if (k8sResources.size() > 1) {
      logger.warn(
          String.format(
              "Resource group with name '%s' contains multiple k8s resources '%s'",
              resourceGroupName,
              k8sResources.stream().map(HasName::name).collect(Collectors.joining(","))));
      throw new MultipleKubernetesResourcesFound(
          String.format(
              "Multiple k8s resources found in the resource group with name '%s'",
              resourceGroupName));
    }

    ContainerServiceManager containerServiceManager =
        crlService.getContainerServiceManager(subscriptionId);
    // don't need to make any assumption about format of the node resource group;
    // just read name of the existing node resource group directly from the aks
    var aks =
        containerServiceManager
            .kubernetesClusters()
            .getByResourceGroup(resourceGroupName, k8sResources.get(0).name());
    return aks.nodeResourceGroup();
  }

  @Scheduled(cron = "${spendreporting.azure.cleanup-cache-cron-schedule}", zone = "GMT+0")
  public void cleanUpAzureSpendReportCache() {
    var azureSpendReportCache =
        cacheManager.getCache(CacheConfiguration.AZURE_SPEND_REPORT_CACHE_NAME);
    if (azureSpendReportCache != null) {
      azureSpendReportCache.clear();
      logger.info(
          "Cache '{}' has been cleaned up.", CacheConfiguration.AZURE_SPEND_REPORT_CACHE_NAME);

    } else {
      logger.warn("Cache '{}' doesn't exist", CacheConfiguration.AZURE_SPEND_REPORT_CACHE_NAME);
    }
  }
}
