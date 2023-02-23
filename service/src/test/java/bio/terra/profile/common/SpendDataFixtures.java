package bio.terra.profile.common;

import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import bio.terra.profile.service.spendreporting.azure.model.SpendDataItem;
import bio.terra.profile.service.spendreporting.azure.model.mapper.SpendDataItemCategoryMapper;
import java.time.OffsetDateTime;
import java.util.List;

public class SpendDataFixtures {
  private SpendDataFixtures() {}

  public static SpendData buildDefaultSpendData() {
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
    return new SpendData(spendDataItems, from, to);
  }

  private static SpendDataItem buildSpendDataItem(
      String resourceType, Double cost, String currency, SpendCategoryType spendCategoryType) {
    return new SpendDataItem(resourceType, cost, currency, spendCategoryType);
  }
}
