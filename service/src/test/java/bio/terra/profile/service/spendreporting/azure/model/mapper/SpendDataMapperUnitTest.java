package bio.terra.profile.service.spendreporting.azure.model.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.model.SpendReportingAggregation;
import bio.terra.profile.model.SpendReportingForDateRange;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import bio.terra.profile.service.spendreporting.azure.model.SpendDataItem;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SpendDataMapperUnitTest extends BaseUnitTest {
  private SpendDataMapper spendDataMapper;

  @BeforeEach
  public void setup() {
    spendDataMapper = new SpendDataMapper();
  }

  @Test
  public void testMapSpendDataWithCategoryAggregation() {
    // arrange
    var computeSpendDataItem =
        buildSpendDataItem(
            SpendDataItemCategoryMapper.AZURE_COMPUTE_RESOURCE_TYPE,
            10.15,
            "USD",
            SpendCategoryType.COMPUTE);
    var storageSpendDataItem =
        buildSpendDataItem(
            SpendDataItemCategoryMapper.AZURE_COMPUTE_RESOURCE_TYPE,
            20.99,
            "USD",
            SpendCategoryType.STORAGE);
    var spendDataItems = List.of(computeSpendDataItem, storageSpendDataItem);
    OffsetDateTime from = OffsetDateTime.now();
    OffsetDateTime to = from.plusDays(30);
    var spendData = new SpendData(spendDataItems, from, to);

    // act
    var spendReport = spendDataMapper.mapSpendData(spendData);
    // assert
    assertNotNull(spendData);
    var spendDetailsWithCategoryAggregation =
        spendReport.getSpendDetails().stream()
            .filter(
                i ->
                    i.getAggregationKey()
                        .equals(SpendReportingAggregation.AggregationKeyEnum.CATEGORY))
            .findFirst();
    assertTrue(spendDetailsWithCategoryAggregation.isPresent());
    var computeItem =
        spendDetailsWithCategoryAggregation.get().getSpendData().stream()
            .filter(i -> i.getCategory().equals(SpendReportingForDateRange.CategoryEnum.COMPUTE))
            .findFirst();
    assertTrue(computeItem.isPresent());
    assertThat(computeItem.get().getCost(), equalTo(String.valueOf(computeSpendDataItem.cost())));
    assertThat(computeItem.get().getCredits(), equalTo("0"));
    assertThat(computeItem.get().getCurrency(), equalTo(computeSpendDataItem.currency()));
    assertThat(
        computeItem.get().getCategory(), equalTo(SpendReportingForDateRange.CategoryEnum.COMPUTE));
    var storageItem =
        spendDetailsWithCategoryAggregation.get().getSpendData().stream()
            .filter(i -> i.getCategory().equals(SpendReportingForDateRange.CategoryEnum.STORAGE))
            .findFirst();
    assertTrue(storageItem.isPresent());
    assertThat(storageItem.get().getCost(), equalTo(String.valueOf(storageSpendDataItem.cost())));
    assertThat(storageItem.get().getCredits(), equalTo("0"));
    assertThat(storageItem.get().getCurrency(), equalTo(storageSpendDataItem.currency()));
    assertThat(
        storageItem.get().getCategory(), equalTo(SpendReportingForDateRange.CategoryEnum.STORAGE));

    var spendSummary = spendReport.getSpendSummary();
    assertNotNull(spendSummary);
    assertThat(
        spendSummary.getCost(),
        equalTo(String.valueOf(computeSpendDataItem.cost() + storageSpendDataItem.cost())));
    assertThat(spendSummary.getCurrency(), equalTo(computeSpendDataItem.currency()));
    assertThat(spendSummary.getStartTime(), equalTo(from.toString()));
    assertThat(spendSummary.getEndTime(), equalTo(to.toString()));
    assertThat(spendSummary.getCredits(), equalTo("0"));
  }

  private SpendDataItem buildSpendDataItem(
      String resourceType, Double cost, String currency, SpendCategoryType spendCategoryType) {
    return new SpendDataItem(resourceType, cost, currency, spendCategoryType);
  }
}
