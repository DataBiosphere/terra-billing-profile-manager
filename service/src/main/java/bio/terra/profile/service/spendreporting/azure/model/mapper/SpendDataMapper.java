package bio.terra.profile.service.spendreporting.azure.model.mapper;

import bio.terra.profile.model.SpendReport;
import bio.terra.profile.model.SpendReportingAggregation;
import bio.terra.profile.model.SpendReportingForDateRange;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.azure.model.SpendData;
import bio.terra.profile.service.spendreporting.azure.model.SpendDataItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SpendDataMapper {
  public SpendReport mapSpendData(SpendData data) {
    var spendData =
        data.getSpendDataItems().stream()
            .map(
                sdi ->
                    new SpendReportingForDateRange()
                        .cost(sdi.cost().toString())
                        .currency(sdi.currency())
                        .credits("0") // Azure doesn't provide such data
                        .category(mapCategory(sdi.spendCategoryType())))
            .toList();

    var spendSummary =
        new SpendReportingForDateRange()
            .cost(
                String.format(
                    "%.2f",
                    data.getSpendDataItems().stream().mapToDouble(SpendDataItem::cost).sum()))
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

  private SpendReportingForDateRange.CategoryEnum mapCategory(SpendCategoryType spendCategoryType) {
    switch (spendCategoryType) {
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
