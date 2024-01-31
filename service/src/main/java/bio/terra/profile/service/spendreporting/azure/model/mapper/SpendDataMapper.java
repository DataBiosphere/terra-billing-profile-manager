package bio.terra.profile.service.spendreporting.azure.model.mapper;

import bio.terra.profile.model.SpendReport;
import bio.terra.profile.model.SpendReportingAggregation;
import bio.terra.profile.model.SpendReportingForDateRange;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import bio.terra.profile.service.spendreporting.azure.model.SpendDataItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SpendDataMapper {
  public static final String COST_FORMAT = "%.2f";

  public SpendReport mapSpendData(SpendData data) {
    var spendData =
        data.getSpendDataItems().stream()
            .map(
                sdi ->
                    buildSpendReportingForDateRange(
                        sdi.cost(), sdi.currency(), sdi.spendCategoryType()))
            .collect(Collectors.groupingBy(SpendReportingForDateRange::getCategory))
            .entrySet()
            .stream()
            .map(
                kvp -> {
                  var categoryTotalCost =
                      kvp.getValue().stream()
                          .map(i -> new BigDecimal(i.getCost()))
                          .reduce(BigDecimal.ZERO, BigDecimal::add);
                  var currency =
                      kvp.getValue().stream()
                          .findFirst()
                          .map(SpendReportingForDateRange::getCurrency)
                          .orElse("n/a");
                  return buildSpendReportingForDateRange(categoryTotalCost, currency, kvp.getKey());
                })
            .toList();

    var spendSummary =
        new SpendReportingForDateRange()
            .cost(
                String.format(
                    COST_FORMAT,
                    data.getSpendDataItems().stream()
                        .map(SpendDataItem::cost)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)))
            .currency(data.getCurrency())
            .credits("0")
            .startTime(data.getFrom().toString())
            .endTime(data.getTo().toString());

    return new SpendReport()
        .spendDetails(
            List.of(
                new SpendReportingAggregation()
                    .aggregationKey(SpendReportingAggregation.AggregationKeyEnum.CATEGORY)
                    .spendData(spendData)))
        .spendSummary(spendSummary);
  }

  private static SpendReportingForDateRange buildSpendReportingForDateRange(
      BigDecimal cost, String currency, SpendCategoryType spendCategoryType) {
    return new SpendReportingForDateRange()
        .cost(String.format(COST_FORMAT, cost))
        .currency(currency)
        .credits("0") // Azure doesn't provide such data
        .category(mapCategory(spendCategoryType));
  }

  private static SpendReportingForDateRange buildSpendReportingForDateRange(
      BigDecimal cost, String currency, SpendReportingForDateRange.CategoryEnum categoryEnum) {
    return new SpendReportingForDateRange()
        .cost(String.format(COST_FORMAT, cost))
        .currency(currency)
        .credits("0") // Azure doesn't provide such data
        .category(categoryEnum);
  }

  private static SpendReportingForDateRange.CategoryEnum mapCategory(
      SpendCategoryType spendCategoryType) {
    switch (spendCategoryType) {
      case WORKSPACE_INFRASTRUCTURE -> {
        return SpendReportingForDateRange.CategoryEnum.WORKSPACEINFRASTRUCTURE;
      }
      case STORAGE -> {
        return SpendReportingForDateRange.CategoryEnum.STORAGE;
      }
      case COMPUTE -> {
        return SpendReportingForDateRange.CategoryEnum.COMPUTE;
      }
      default -> {
        return SpendReportingForDateRange.CategoryEnum.OTHER;
      }
    }
  }
}
