package bio.terra.profile.service.spendreporting.azure.model;

public record SpendDataItem(
    String resourceType, Double cost, String currency, SpendCategoryType spendCategoryType) {}
