package bio.terra.profile.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spendreporting")
public class SpendReportingConfig {
  private Azure azure;

  public Azure getAzure() {
    return azure;
  }

  public void setAzure(final Azure azure) {
    this.azure = azure;
  }

  public static class Azure {
    private int maxDateRangeDays;

    public int getMaxDateRangeDays() {
      return maxDateRangeDays;
    }

    public void setMaxDateRangeDays(final int maxDateRangeDays) {
      this.maxDateRangeDays = maxDateRangeDays;
    }
  }
}
