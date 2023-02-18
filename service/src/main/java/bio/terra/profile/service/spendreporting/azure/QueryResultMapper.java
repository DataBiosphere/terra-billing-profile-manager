package bio.terra.profile.service.spendreporting.azure;

import bio.terra.profile.service.spendreporting.azure.exception.UnexpectedCostManagementQueryResponse;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import bio.terra.profile.service.spendreporting.azure.model.SpendDataItem;
import com.azure.resourcemanager.costmanagement.models.QueryColumn;
import com.azure.resourcemanager.costmanagement.models.QueryResult;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class QueryResultMapper {
  public static final String COST_COLUMN_NAME = "cost";
  public static final String RESOURCE_TYPE_COLUMN_NAME = "resourcetype";
  public static final String CURRENCY_COLUMN_NAME = "currency";

  public static final int COST_COLUMN_INDEX = 0;
  public static final int RESOURCE_TYPE_COLUMN_INDEX = 1;
  public static final int CURRENCY_COLUMN_INDEX = 2;

  private static final int EXPECTED_NUMBER_OF_COLUMNS = 3;

  private SpendDataItemCategoryMapper spendDataItemCategoryMapper;

  public QueryResultMapper(SpendDataItemCategoryMapper spendDataItemCategoryMapper) {
    this.spendDataItemCategoryMapper = spendDataItemCategoryMapper;
  }

  public SpendData mapQueryResult(QueryResult queryResult) {
    throwIfDataFormatIsNotValid(queryResult);

    var spendItems =
        queryResult.rows().stream()
            .map(
                r ->
                    new SpendDataItem(
                        r.get(RESOURCE_TYPE_COLUMN_INDEX).toString(),
                        Double.parseDouble(r.get(COST_COLUMN_INDEX).toString()),
                        r.get(CURRENCY_COLUMN_INDEX).toString(),
                        spendDataItemCategoryMapper.mapResourceCategory(
                            r.get(RESOURCE_TYPE_COLUMN_INDEX).toString())))
            .collect(Collectors.toList());

    return new SpendData(spendItems);
  }

  private void throwIfDataFormatIsNotValid(QueryResult queryResult) {
    if (queryResult.columns().size() != EXPECTED_NUMBER_OF_COLUMNS) {
      throw new UnexpectedCostManagementQueryResponse(
          String.format(
              "Unexpected number of columns: actual=%s, expected=%s",
              queryResult.columns().size(), EXPECTED_NUMBER_OF_COLUMNS));
    }
    if (!(StringUtils.equalsIgnoreCase(
            queryResult.columns().get(COST_COLUMN_INDEX).name(), COST_COLUMN_NAME)
        || StringUtils.equalsIgnoreCase(
            queryResult.columns().get(COST_COLUMN_INDEX).name(), RESOURCE_TYPE_COLUMN_NAME)
        || StringUtils.equalsIgnoreCase(
            queryResult.columns().get(COST_COLUMN_INDEX).name(), CURRENCY_COLUMN_NAME))) {
      throw new UnexpectedCostManagementQueryResponse(
          String.format(
              "One of the expected columns not found. Columns=[%s]",
              queryResult.columns().stream()
                  .map(QueryColumn::name)
                  .collect(Collectors.joining(","))));
    }
  }
}
