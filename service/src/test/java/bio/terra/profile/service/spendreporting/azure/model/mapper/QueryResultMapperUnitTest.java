package bio.terra.profile.service.spendreporting.azure.model.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.service.spendreporting.azure.exception.UnexpectedCostManagementQueryResponse;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import com.azure.resourcemanager.costmanagement.models.QueryColumn;
import com.azure.resourcemanager.costmanagement.models.QueryResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryResultMapperUnitTest extends BaseUnitTest {
  private QueryResultMapper queryResultMapper;

  @BeforeEach
  void setup() {
    queryResultMapper = new QueryResultMapper(new SpendDataItemCategoryMapper());
  }

  @Test
  void testMapQueryResultWithWrongNumberOfColumnsThrowsException() {
    QueryResult queryResult = mock(QueryResult.class);
    var listOfOnlyOneColumn = List.of(new QueryColumn().withName("column1"));
    when(queryResult.columns()).thenReturn(listOfOnlyOneColumn);

    assertThrows(
        UnexpectedCostManagementQueryResponse.class,
        () -> queryResultMapper.mapQueryResult(queryResult));
  }

  @Test
  void testMapQueryResultWithUnexpectedColumnNamesThrowsException() {
    QueryResult queryResult = mock(QueryResult.class);

    var listOfOnlyOneColumn =
        List.of(
            new QueryColumn().withName("column1"),
            new QueryColumn().withName("column2"),
            new QueryColumn().withName("column3"));
    when(queryResult.columns()).thenReturn(listOfOnlyOneColumn);

    assertThrows(
        UnexpectedCostManagementQueryResponse.class,
        () -> queryResultMapper.mapQueryResult(queryResult));
  }

  @Test
  void testMapQueryResultWithShuffledColumnsThrowsException() {
    QueryResult queryResult = mock(QueryResult.class);

    var currencyColumn = new QueryColumn().withName(QueryResultMapper.CURRENCY_COLUMN_NAME);
    var costColumn = new QueryColumn().withName(QueryResultMapper.COST_COLUMN_NAME);
    var resourceTypeColumn =
        new QueryColumn().withName(QueryResultMapper.RESOURCE_TYPE_COLUMN_NAME);

    var columns = mock(List.class);
    when(columns.get(QueryResultMapper.COST_COLUMN_INDEX)).thenReturn(resourceTypeColumn);
    when(columns.get(QueryResultMapper.CURRENCY_COLUMN_INDEX)).thenReturn(costColumn);
    when(columns.get(QueryResultMapper.RESOURCE_TYPE_COLUMN_INDEX)).thenReturn(currencyColumn);

    when(queryResult.columns()).thenReturn(columns);

    assertThrows(
        UnexpectedCostManagementQueryResponse.class,
        () -> queryResultMapper.mapQueryResult(queryResult));
  }

  @Test
  void testMapValidQueryResultSuccess() {
    List<List<Object>> rows = new ArrayList<>();
    List<Object> row =
        List.of(15.23, SpendDataItemCategoryMapper.AZURE_COMPUTE_RESOURCE_TYPE, "USD");
    rows.add(row);

    var queryResult = mockValidQueryResult(rows);

    SpendData result = queryResultMapper.mapQueryResult(queryResult);
    assertNotNull(result);
    assertThat(result.getSpendDataItems().size(), equalTo(1));
    assertThat(
        result.getSpendDataItems().get(0).cost(),
        equalTo(row.get(QueryResultMapper.COST_COLUMN_INDEX)));
    assertThat(
        result.getSpendDataItems().get(0).resourceType(),
        equalTo(row.get(QueryResultMapper.RESOURCE_TYPE_COLUMN_INDEX)));
    assertThat(
        result.getSpendDataItems().get(0).currency(),
        equalTo(row.get(QueryResultMapper.CURRENCY_COLUMN_INDEX)));
    assertThat(
        result.getSpendDataItems().get(0).spendCategoryType(), equalTo(SpendCategoryType.COMPUTE));
  }

  private QueryResult mockValidQueryResult(List<List<Object>> rows) {
    QueryResult queryResult = mock(QueryResult.class);

    var currencyColumn = new QueryColumn().withName(QueryResultMapper.CURRENCY_COLUMN_NAME);
    var costColumn = new QueryColumn().withName(QueryResultMapper.COST_COLUMN_NAME);
    var resourceTypeColumn =
        new QueryColumn().withName(QueryResultMapper.RESOURCE_TYPE_COLUMN_NAME);

    var columns = mock(List.class);
    when(columns.get(QueryResultMapper.COST_COLUMN_INDEX)).thenReturn(costColumn);
    when(columns.get(QueryResultMapper.CURRENCY_COLUMN_INDEX)).thenReturn(currencyColumn);
    when(columns.get(QueryResultMapper.RESOURCE_TYPE_COLUMN_INDEX)).thenReturn(resourceTypeColumn);
    when(columns.size()).thenReturn(3);

    when(queryResult.rows()).thenReturn(rows);

    when(queryResult.columns()).thenReturn(columns);
    return queryResult;
  }
}
