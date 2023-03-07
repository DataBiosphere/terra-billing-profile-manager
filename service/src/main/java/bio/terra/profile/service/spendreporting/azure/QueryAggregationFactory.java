package bio.terra.profile.service.spendreporting.azure;

import com.azure.resourcemanager.costmanagement.models.FunctionType;
import com.azure.resourcemanager.costmanagement.models.QueryAggregation;
import java.util.HashMap;
import java.util.Map;

public class QueryAggregationFactory {
  private QueryAggregationFactory() {}

  public static Map<String, QueryAggregation> buildDefault() {
    Map<String, QueryAggregation> queryAggregation = new HashMap<>();
    queryAggregation.put(
        "totalCost", new QueryAggregation().withName("Cost").withFunction(FunctionType.SUM));
    return queryAggregation;
  }
}
