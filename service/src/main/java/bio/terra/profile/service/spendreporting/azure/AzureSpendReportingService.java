package bio.terra.profile.service.spendreporting.azure;

import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.spendreporting.azure.exception.KubernetesResourceNotFound;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import com.azure.core.http.rest.Response;
import com.azure.resourcemanager.costmanagement.models.QueryResult;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.GenericResource;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

@Component
public class AzureSpendReportingService {
  public static final String K8S_RESOURCE_GROUP_NAME_PREFIX = "MC";
  public static final String AZURE_KUBERNETES_RESOURCE_TYPE = "microsoft.container";
  private final AzureCostManagementQuery azureCostManagementQuery;
  private final CrlService crlService;
  private final QueryResultMapper queryResultMapper;

  public AzureSpendReportingService(
      AzureCostManagementQuery azureCostManagementQuery,
      CrlService crlService,
      QueryResultMapper queryResultMapper) {
    this.azureCostManagementQuery = azureCostManagementQuery;
    this.crlService = crlService;
    this.queryResultMapper = queryResultMapper;
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
  public SpendData getBillingProjectSpendData(
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
                        billingProfile.getRequiredTenantId(),
                        billingProfile.getRequiredManagedResourceGroupId(),
                        from,
                        to)),
            CompletableFuture.supplyAsync(
                () ->
                    querySpendDataForResourceGroup(
                        billingProfile.getRequiredTenantId(), k8sNodeResourceGroup, from, to)));

    List<SpendData> spendDataList =
        azureCostManagementQueriesFutures.stream().map(CompletableFuture::join).toList();

    return new SpendData(
        spendDataList.stream().flatMap(sp -> sp.getSpendDataItems().stream()).toList());
  }

  private SpendData querySpendDataForResourceGroup(
      UUID subscriptionId, String resourceGroupName, OffsetDateTime from, OffsetDateTime to) {
    Response<QueryResult> costQueryResponse =
        azureCostManagementQuery.resourceGroupCostQueryWithResourceTypeGrouping(
            subscriptionId, resourceGroupName, from, to);
    if (isK8sResourceGroup(resourceGroupName)) {
      return queryResultMapper.mapQueryResult(
          costQueryResponse.getValue(), SpendCategoryType.COMPUTE);
    } else {
      return queryResultMapper.mapQueryResult(costQueryResponse.getValue());
    }
  }

  private boolean isK8sResourceGroup(String resourceGroupName) {
    return resourceGroupName.startsWith(K8S_RESOURCE_GROUP_NAME_PREFIX);
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
    Optional<GenericResource> k8sResource =
        resourceManager.genericResources().listByResourceGroup(resourceGroupName).stream()
            .filter(gr -> gr.resourceType().startsWith(AZURE_KUBERNETES_RESOURCE_TYPE))
            .findFirst();
    if (k8sResource.isEmpty()) {
      throw new KubernetesResourceNotFound(
          String.format(
              "Cannot find K8s resource in the managed resource group with name '%s'",
              resourceGroupName));
    }

    // by default, it follows this pattern - "MC_<resourcegroupname>_<clustername>_<location>"
    // where resourcegroupname is the name of managed resource group, clustername is the name of K8s
    // cluster, location is the region name.
    return String.format(
        "%s_%s_%s_%s",
        K8S_RESOURCE_GROUP_NAME_PREFIX,
        resourceGroupName,
        k8sResource.get().name(),
        k8sResource.get().regionName());
  }
}
