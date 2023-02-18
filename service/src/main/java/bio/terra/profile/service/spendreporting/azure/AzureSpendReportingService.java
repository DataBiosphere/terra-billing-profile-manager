package bio.terra.profile.service.spendreporting.azure;

import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

@Component
public class AzureSpendReportingService {
  private final AzureCostManagementQuery azureCostManagementQuery;

  public AzureSpendReportingService(AzureCostManagementQuery azureCostManagementQuery) {
    this.azureCostManagementQuery = azureCostManagementQuery;
  }

  //Resources related to an Azure billing project reside in the 2 resource groups:
  //1st - managed resource group related to the Terra managed app
  //2nd - K8s node resource group
  public SpendData getBillingProjectSpendData(
      UUID subscriptionId,
      String resourceGroupName,
      String k8sNodeResourceGroup,
      OffsetDateTime from,
      OffsetDateTime to) {
    List<CompletableFuture<SpendData>> azureCostManagementQueriesFutures =
        List.of(
            CompletableFuture.supplyAsync(
                () -> querySpendDataForResourceGroup(subscriptionId, resourceGroupName, from, to)),
            CompletableFuture.supplyAsync(
                () ->
                    querySpendDataForResourceGroup(
                        subscriptionId, k8sNodeResourceGroup, from, to)));

    List<SpendData> spendDataList =
        azureCostManagementQueriesFutures.stream().map(CompletableFuture::join).toList();

    return new SpendData(
        spendDataList.stream().flatMap(sp -> sp.getSpendDataItems().stream()).toList());
  }

  private SpendData querySpendDataForResourceGroup(
      UUID subscriptionId, String resourceGroupName, OffsetDateTime from, OffsetDateTime to) {
    return azureCostManagementQuery.resourceGroupCostQueryWithResourceTypeGrouping(
        subscriptionId, resourceGroupName, from, to);
  }
}
