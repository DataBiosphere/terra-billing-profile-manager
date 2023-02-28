package bio.terra.profile.common;

import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import bio.terra.profile.service.spendreporting.azure.model.SpendDataItem;
import bio.terra.profile.service.spendreporting.azure.model.mapper.SpendDataItemCategoryMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class SpendDataFixtures {
  public static final String DEFAULT_COMPUTE1_COST = "10.15";
  public static final String DEFAULT_COMPUTE2_COST = "100.15";
  public static final String DEFAULT_STORAGE_COST = "20.99";
  public static final String DEFAULT_CURRENCY = "USD";
  public static final OffsetDateTime DEFAULT_FROM = OffsetDateTime.now();
  public static final OffsetDateTime DEFAULT_TO = DEFAULT_FROM.plusDays(30);

  private SpendDataFixtures() {}

  public static SpendData buildDefaultSpendData() {
    var computeSpendDataItem =
        buildSpendDataItem(
            SpendDataItemCategoryMapper.AZURE_COMPUTE_RESOURCE_TYPE,
            Double.parseDouble(DEFAULT_COMPUTE1_COST),
            DEFAULT_CURRENCY,
            SpendCategoryType.COMPUTE);
    var computeSpendDataItem2 =
        buildSpendDataItem(
            SpendDataItemCategoryMapper.AZURE_COMPUTE_RESOURCE_TYPE,
            Double.parseDouble(DEFAULT_COMPUTE2_COST),
            DEFAULT_CURRENCY,
            SpendCategoryType.COMPUTE);
    var storageSpendDataItem =
        buildSpendDataItem(
            SpendDataItemCategoryMapper.AZURE_COMPUTE_RESOURCE_TYPE,
            Double.parseDouble(DEFAULT_STORAGE_COST),
            DEFAULT_CURRENCY,
            SpendCategoryType.STORAGE);
    var spendDataItems = List.of(computeSpendDataItem, computeSpendDataItem2, storageSpendDataItem);
    return new SpendData(spendDataItems, DEFAULT_FROM, DEFAULT_TO);
  }

  public static SpendData buildEmptySpendData() {
    return new SpendData(new ArrayList<>(), DEFAULT_FROM, DEFAULT_TO);
  }

  private static SpendDataItem buildSpendDataItem(
      String resourceType, Double cost, String currency, SpendCategoryType spendCategoryType) {
    return new SpendDataItem(resourceType, cost, currency, spendCategoryType);
  }
}
