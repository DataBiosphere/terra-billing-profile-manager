package bio.terra.profile.service.spendreporting;

import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.spendreporting.model.SpendData;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.resourcemanager.costmanagement.models.ExportType;
import com.azure.resourcemanager.costmanagement.models.QueryColumnType;
import com.azure.resourcemanager.costmanagement.models.QueryDataset;
import com.azure.resourcemanager.costmanagement.models.QueryDefinition;
import com.azure.resourcemanager.costmanagement.models.QueryGrouping;
import com.azure.resourcemanager.costmanagement.models.QueryResult;
import com.azure.resourcemanager.costmanagement.models.QueryTimePeriod;
import com.azure.resourcemanager.costmanagement.models.TimeframeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AzureCostManagementQuery {
  private static final String GROUPING_BY_RESOURCE_ID = "ResourceId";
  private static final String GROUPING_BY_RESOURCE_Type = "ResourceType";

  private final CrlService crlService;
  private final QueryResultMapper queryResultMapper;

  public AzureCostManagementQuery(CrlService crlService, QueryResultMapper queryResultMapper) {
    this.crlService = crlService;
    this.queryResultMapper = queryResultMapper;
  }

  public SpendData resourceGroupCostQueryWithResourceIdGrouping(
      UUID subscriptionId, String resourceGroupName, OffsetDateTime from, OffsetDateTime to) {
    return resourceGroupCostQuery(
        subscriptionId, resourceGroupName, from, to, GROUPING_BY_RESOURCE_ID);
  }

  public SpendData resourceGroupCostQueryWithResourceTypeGrouping(
      UUID subscriptionId, String resourceGroupName, OffsetDateTime from, OffsetDateTime to) {
    return resourceGroupCostQuery(
        subscriptionId, resourceGroupName, from, to, GROUPING_BY_RESOURCE_Type);
  }

  private SpendData resourceGroupCostQuery(
      UUID subscriptionId,
      String resourceGroupName,
      OffsetDateTime from,
      OffsetDateTime to,
      String groupingName) {
    var costManagementManager = crlService.getCostManagementManager(subscriptionId);

    Response<QueryResult> result =
        costManagementManager
            .queries()
            .usageWithResponse(
                UsageScopeFactory.buildResourceGroupUsageScope(
                    subscriptionId.toString(), resourceGroupName),
                new QueryDefinition()
                    .withType(ExportType.ACTUAL_COST)
                    .withTimeframe(TimeframeType.CUSTOM)
                    .withTimePeriod(new QueryTimePeriod().withFrom(from).withTo(to))
                    .withDataset(
                        new QueryDataset()
                            .withAggregation(QueryAggregationFactory.buildDefault())
                            .withGrouping(
                                List.of(
                                    new QueryGrouping()
                                        .withType(QueryColumnType.DIMENSION)
                                        .withName(groupingName)))),
                Context.NONE);
    return queryResultMapper.mapQueryResult(result.getValue());
  }
}
