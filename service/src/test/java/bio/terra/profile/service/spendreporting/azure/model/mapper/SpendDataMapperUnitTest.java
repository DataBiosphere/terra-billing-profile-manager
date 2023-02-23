package bio.terra.profile.service.spendreporting.azure.model.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.common.SpendDataFixtures;
import bio.terra.profile.model.SpendReportingAggregation;
import bio.terra.profile.model.SpendReportingForDateRange;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
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
    var spendData = SpendDataFixtures.buildDefaultSpendData();

    var spendReport = spendDataMapper.mapSpendData(spendData);

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
    var computeSpendDataItem =
        spendData.getSpendDataItems().stream()
            .filter(sdi -> sdi.spendCategoryType().equals(SpendCategoryType.COMPUTE))
            .findFirst();
    assertThat(
        computeItem.get().getCost(), equalTo(String.valueOf(computeSpendDataItem.get().cost())));
    assertThat(computeItem.get().getCredits(), equalTo("0"));
    assertThat(computeItem.get().getCurrency(), equalTo(computeSpendDataItem.get().currency()));
    assertThat(
        computeItem.get().getCategory(), equalTo(SpendReportingForDateRange.CategoryEnum.COMPUTE));
    var storageItem =
        spendDetailsWithCategoryAggregation.get().getSpendData().stream()
            .filter(i -> i.getCategory().equals(SpendReportingForDateRange.CategoryEnum.STORAGE))
            .findFirst();
    assertTrue(storageItem.isPresent());
    var storageSpendDataItem =
        spendData.getSpendDataItems().stream()
            .filter(sdi -> sdi.spendCategoryType().equals(SpendCategoryType.STORAGE))
            .findFirst();
    assertThat(
        storageItem.get().getCost(), equalTo(String.valueOf(storageSpendDataItem.get().cost())));
    assertThat(storageItem.get().getCredits(), equalTo("0"));
    assertThat(storageItem.get().getCurrency(), equalTo(storageSpendDataItem.get().currency()));
    assertThat(
        storageItem.get().getCategory(), equalTo(SpendReportingForDateRange.CategoryEnum.STORAGE));

    var spendSummary = spendReport.getSpendSummary();
    assertNotNull(spendSummary);
    assertThat(
        spendSummary.getCost(),
        equalTo(
            String.valueOf(computeSpendDataItem.get().cost() + storageSpendDataItem.get().cost())));
    assertThat(spendSummary.getCurrency(), equalTo(computeSpendDataItem.get().currency()));
    assertThat(spendSummary.getStartTime(), equalTo(spendData.getFrom().toString()));
    assertThat(spendSummary.getEndTime(), equalTo(spendData.getTo().toString()));
    assertThat(spendSummary.getCredits(), equalTo("0"));
  }
}
