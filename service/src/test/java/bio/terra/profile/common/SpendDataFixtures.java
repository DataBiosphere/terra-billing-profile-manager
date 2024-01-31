package bio.terra.profile.common;

import bio.terra.profile.service.spendreporting.azure.model.AzureResourceProviderType;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import bio.terra.profile.service.spendreporting.azure.model.SpendDataItem;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class SpendDataFixtures {
  public static final BigDecimal DEFAULT_COMPUTE1_COST = new BigDecimal("10.15");
  public static final BigDecimal DEFAULT_COMPUTE2_COST = new BigDecimal("100.15");
  public static final BigDecimal DEFAULT_STORAGE_COST = new BigDecimal("20.99");
  public static final String DEFAULT_CURRENCY = "USD";
  public static final OffsetDateTime DEFAULT_FROM = OffsetDateTime.now();
  public static final OffsetDateTime DEFAULT_TO = DEFAULT_FROM.plusDays(30);

  private SpendDataFixtures() {}

  public static SpendData buildDefaultSpendData() {
    var computeSpendDataItem =
        buildSpendDataItem(
            AzureResourceProviderType.COMPUTE.getValue(),
            DEFAULT_COMPUTE1_COST,
            DEFAULT_CURRENCY,
            SpendCategoryType.COMPUTE);
    var computeSpendDataItem2 =
        buildSpendDataItem(
            AzureResourceProviderType.COMPUTE.getValue(),
            DEFAULT_COMPUTE2_COST,
            DEFAULT_CURRENCY,
            SpendCategoryType.COMPUTE);
    var storageSpendDataItem =
        buildSpendDataItem(
            AzureResourceProviderType.STORAGE.getValue(),
            DEFAULT_STORAGE_COST,
            DEFAULT_CURRENCY,
            SpendCategoryType.STORAGE);
    var spendDataItems = List.of(computeSpendDataItem, computeSpendDataItem2, storageSpendDataItem);
    return new SpendData(spendDataItems, DEFAULT_FROM, DEFAULT_TO);
  }

  public static SpendData buildEmptySpendData() {
    return buildSpendData(new ArrayList<>(), DEFAULT_FROM, DEFAULT_TO);
  }

  public static SpendData buildSpendData(
      List<SpendDataItem> spendDataItems, OffsetDateTime from, OffsetDateTime to) {
    return new SpendData(spendDataItems, from, to);
  }

  public static SpendData buildSingleItemSpendData(
      String resourceType,
      BigDecimal cost,
      String currency,
      SpendCategoryType categoryType,
      OffsetDateTime from,
      OffsetDateTime to) {
    return new SpendData(
        List.of(new SpendDataItem(resourceType, cost, currency, categoryType)), from, to);
  }

  private static SpendDataItem buildSpendDataItem(
      String resourceType, BigDecimal cost, String currency, SpendCategoryType spendCategoryType) {
    return new SpendDataItem(resourceType, cost, currency, spendCategoryType);
  }
}
