package bio.terra.profile.service.spendreporting.azure;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.service.spendreporting.azure.exception.UnexpectedCostManagementQueryResponse;
import com.azure.resourcemanager.costmanagement.models.QueryColumn;
import com.azure.resourcemanager.costmanagement.models.QueryResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QueryResultMapperUnitTest extends BaseUnitTest {
  private QueryResultMapper queryResultMapper;

  @BeforeEach
  public void setup() {
    queryResultMapper = new QueryResultMapper(new SpendDataItemCategoryMapper());
  }

  @Test
  public void testMapQueryResultWithWrongNumberOfColumnsThrowsException() {
    QueryResult queryResult = mock(QueryResult.class);
    var listOfOnlyOneColumn = List.of(new QueryColumn().withName("column1"));
    when(queryResult.columns()).thenReturn(listOfOnlyOneColumn);

    assertThrows(
        UnexpectedCostManagementQueryResponse.class,
        () -> queryResultMapper.mapQueryResult(queryResult));
  }

  @Test
  public void testMapQueryResultWithUnexpectedColumnNamesThrowsException() {
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
  public void testMapQueryResultWithShuffledColumnsThrowsException() {
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
  public void testMapValidQueryResultSuccess() {
    QueryResult queryResult = mock(QueryResult.class);

    var currencyColumn = new QueryColumn().withName(QueryResultMapper.CURRENCY_COLUMN_NAME);
    var costColumn = new QueryColumn().withName(QueryResultMapper.COST_COLUMN_NAME);
    var resourceTypeColumn =
        new QueryColumn().withName(QueryResultMapper.RESOURCE_TYPE_COLUMN_NAME);

    var columns = mock(List.class);
    when(columns.get(QueryResultMapper.COST_COLUMN_INDEX)).thenReturn(costColumn);
    when(columns.get(QueryResultMapper.CURRENCY_COLUMN_INDEX)).thenReturn(currencyColumn);
    when(columns.get(QueryResultMapper.RESOURCE_TYPE_COLUMN_INDEX)).thenReturn(resourceTypeColumn);

    when(queryResult.columns()).thenReturn(columns);

    assertThrows(
        UnexpectedCostManagementQueryResponse.class,
        () -> queryResultMapper.mapQueryResult(queryResult));
  }
}
