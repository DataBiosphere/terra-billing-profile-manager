package bio.terra.profile.service.spendreporting.azure.model.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.common.SpendDataFixtures;
import bio.terra.profile.model.SpendReportingAggregation;
import bio.terra.profile.model.SpendReportingForDateRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpendDataMapperUnitTest extends BaseUnitTest {
  private SpendDataMapper spendDataMapper;

  @BeforeEach
  void setup() {
    spendDataMapper = new SpendDataMapper();
  }

  @Test
  void testMapSpendDataWithCategoryAggregation() {
    var spendData = SpendDataFixtures.buildDefaultSpendData();

    var spendReport = spendDataMapper.mapSpendData(spendData);

    assertNotNull(spendReport);
    var spendDetailsWithCategoryAggregation =
        spendReport.getSpendDetails().stream()
            .filter(
                i ->
                    i.getAggregationKey()
                        .equals(SpendReportingAggregation.AggregationKeyEnum.CATEGORY))
            .findFirst();
    assertTrue(spendDetailsWithCategoryAggregation.isPresent());

    var aggregatedComputeItem =
        spendDetailsWithCategoryAggregation.get().getSpendData().stream()
            .filter(i -> i.getCategory().equals(SpendReportingForDateRange.CategoryEnum.COMPUTE))
            .findFirst();
    assertTrue(aggregatedComputeItem.isPresent());
    assertThat(
        aggregatedComputeItem.get().getCategory(),
        equalTo(SpendReportingForDateRange.CategoryEnum.COMPUTE));
    assertThat(
        aggregatedComputeItem.get().getCost(),
        equalTo(
            String.format(
                SpendDataMapper.COST_FORMAT,
                SpendDataFixtures.DEFAULT_COMPUTE1_COST.add(
                    SpendDataFixtures.DEFAULT_COMPUTE2_COST))));
    assertThat(
        aggregatedComputeItem.get().getCurrency(), equalTo(SpendDataFixtures.DEFAULT_CURRENCY));
    assertNull(aggregatedComputeItem.get().getStartTime());
    assertNull(aggregatedComputeItem.get().getEndTime());
    assertThat(aggregatedComputeItem.get().getCredits(), equalTo("0"));

    var aggregatedStorageItem =
        spendDetailsWithCategoryAggregation.get().getSpendData().stream()
            .filter(i -> i.getCategory().equals(SpendReportingForDateRange.CategoryEnum.STORAGE))
            .findFirst();
    assertTrue(aggregatedStorageItem.isPresent());
    assertThat(
        aggregatedStorageItem.get().getCategory(),
        equalTo(SpendReportingForDateRange.CategoryEnum.STORAGE));
    assertThat(
        aggregatedStorageItem.get().getCost(),
        equalTo(
            String.format(SpendDataMapper.COST_FORMAT, SpendDataFixtures.DEFAULT_STORAGE_COST)));
    assertThat(
        aggregatedStorageItem.get().getCurrency(), equalTo(SpendDataFixtures.DEFAULT_CURRENCY));
    assertNull(aggregatedStorageItem.get().getStartTime());
    assertNull(aggregatedStorageItem.get().getEndTime());
    assertThat(aggregatedStorageItem.get().getCredits(), equalTo("0"));

    var spendSummary = spendReport.getSpendSummary();
    assertNotNull(spendSummary);
    assertThat(
        spendSummary.getCost(),
        equalTo(
            String.valueOf(
                String.format(
                    SpendDataMapper.COST_FORMAT,
                    Double.parseDouble(aggregatedComputeItem.get().getCost())
                        + Double.parseDouble(aggregatedStorageItem.get().getCost())))));
    assertThat(spendSummary.getCurrency(), equalTo(aggregatedComputeItem.get().getCurrency()));
    assertThat(spendSummary.getStartTime(), equalTo(spendData.getFrom().toString()));
    assertThat(spendSummary.getEndTime(), equalTo(spendData.getTo().toString()));
    assertThat(spendSummary.getCredits(), equalTo("0"));
  }

  @Test
  void testMapEmptySpendData() {
    var emptySpendData = SpendDataFixtures.buildEmptySpendData();

    var spendReport = spendDataMapper.mapSpendData(emptySpendData);

    assertNotNull(spendReport);
    assertNotNull(spendReport.getSpendDetails().get(0));
    assertThat(
        spendReport.getSpendDetails().get(0).getAggregationKey(),
        equalTo(SpendReportingAggregation.AggregationKeyEnum.CATEGORY));
    assertNotNull(spendReport.getSpendDetails().get(0).getSpendData());
    assertTrue(spendReport.getSpendDetails().get(0).getSpendData().isEmpty());
    assertNotNull(spendReport.getSpendSummary());
    assertThat(
        spendReport.getSpendSummary().getCost(),
        equalTo(String.format(SpendDataMapper.COST_FORMAT, Double.parseDouble("0"))));
    assertThat(spendReport.getSpendSummary().getCredits(), equalTo("0"));
    assertThat(
        spendReport.getSpendSummary().getStartTime(),
        equalTo(SpendDataFixtures.DEFAULT_FROM.toString()));
    assertThat(
        spendReport.getSpendSummary().getEndTime(),
        equalTo(SpendDataFixtures.DEFAULT_TO.toString()));
    assertThat(spendReport.getSpendSummary().getCurrency(), equalTo("n/a"));
  }
}
