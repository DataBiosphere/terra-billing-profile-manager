package bio.terra.profile.service.spendreporting.azure.model;

import java.math.BigDecimal;

public record SpendDataItem(
    String resourceType, BigDecimal cost, String currency, SpendCategoryType spendCategoryType) {}
