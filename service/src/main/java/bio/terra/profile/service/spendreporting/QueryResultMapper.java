package bio.terra.profile.service.spendreporting;

import bio.terra.profile.service.spendreporting.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.model.SpendData;
import bio.terra.profile.service.spendreporting.model.SpendDataItem;
import com.azure.resourcemanager.costmanagement.models.QueryResult;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class QueryResultMapper {
  private static final int ROW_COST_COLUMN_INDEX = 0;
  private static final int ROW_RESOURCE_TYPE_COLUMN_INDEX = 1;
  private static final int ROW_CURRENCY_COLUMN_INDEX = 2;

  private static final String AZURE_COMPUTE_RESOURCE_TYPE = "microsoft.compute";
  private static final String AZURE_STORAGE_RESOURCE_TYPE = "microsoft.storage";

  public SpendData mapQueryResult(QueryResult queryResult) {
    var spendItems =
        queryResult.rows().stream()
            .map(
                r ->
                    new SpendDataItem(
                        r.get(ROW_RESOURCE_TYPE_COLUMN_INDEX).toString(),
                        Double.parseDouble(r.get(ROW_COST_COLUMN_INDEX).toString()),
                        r.get(ROW_CURRENCY_COLUMN_INDEX).toString(),
                        mapResourceCategory(r.get(ROW_RESOURCE_TYPE_COLUMN_INDEX).toString())))
            .collect(Collectors.toList());

    return new SpendData(spendItems);
  }

  private SpendCategoryType mapResourceCategory(String resourceCategory) {
    if (resourceCategory.startsWith(AZURE_COMPUTE_RESOURCE_TYPE)) {
      return SpendCategoryType.COMPUTE;
    } else if (resourceCategory.startsWith(AZURE_STORAGE_RESOURCE_TYPE)) {
      return SpendCategoryType.STORAGE;
    } else {
      return SpendCategoryType.OTHER;
    }
  }
}
