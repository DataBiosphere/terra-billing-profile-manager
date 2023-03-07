package bio.terra.profile.common;

import bio.terra.profile.model.SpendReport;
import bio.terra.profile.model.SpendReportingAggregation;
import bio.terra.profile.model.SpendReportingForDateRange;
import java.time.OffsetDateTime;
import java.util.List;

public class SpendReportFixtures {
  private SpendReportFixtures() {}

  public static SpendReport buildDefaultSpendReport() {
    var computeSpendReportingForDataRange =
        new SpendReportingForDateRange()
            .cost("10.15")
            .credits("0")
            .currency("USD")
            .startTime(OffsetDateTime.now().toString())
            .endTime(OffsetDateTime.now().plusDays(30).toString());

    var storageSpendReportingForDataRange =
        new SpendReportingForDateRange()
            .cost("20.99")
            .credits("0")
            .currency("USD")
            .startTime(OffsetDateTime.now().toString())
            .endTime(OffsetDateTime.now().plusDays(30).toString());

    return new SpendReport()
        .spendSummary(
            new SpendReportingForDateRange()
                .cost("31.14")
                .credits("0")
                .currency("USD")
                .startTime(OffsetDateTime.now().toString())
                .endTime(OffsetDateTime.now().plusDays(30).toString()))
        .spendDetails(
            List.of(
                new SpendReportingAggregation()
                    .spendData(
                        List.of(
                            computeSpendReportingForDataRange, storageSpendReportingForDataRange))
                    .aggregationKey(SpendReportingAggregation.AggregationKeyEnum.CATEGORY)));
  }
}
