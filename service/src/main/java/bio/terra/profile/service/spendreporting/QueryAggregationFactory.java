package bio.terra.profile.service.spendreporting;

import com.azure.resourcemanager.costmanagement.models.FunctionType;
import com.azure.resourcemanager.costmanagement.models.QueryAggregation;
import java.util.HashMap;
import java.util.Map;

public class QueryAggregationFactory {
  public static Map<String, QueryAggregation> buildDefault() {
    return new HashMap<>() {
      {
        put("totalCost", new QueryAggregation().withName("Cost").withFunction(FunctionType.SUM));
      }

      {
        put(
            "totalCostUSD",
            new QueryAggregation().withName("CostUSD").withFunction(FunctionType.SUM));
      }
    };
  }
}
