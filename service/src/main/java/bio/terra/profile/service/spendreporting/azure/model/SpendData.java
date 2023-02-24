package bio.terra.profile.service.spendreporting.azure.model;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class SpendData {
  private final List<SpendDataItem> spendDataItems;
  private final OffsetDateTime from;
  private final OffsetDateTime to;

  public SpendData(List<SpendDataItem> spendDataItems, OffsetDateTime from, OffsetDateTime to) {
    this.spendDataItems = spendDataItems;
    this.from = from;
    this.to = to;
  }

  public List<SpendDataItem> getSpendDataItems() {
    return Collections.unmodifiableList(spendDataItems);
  }

  public OffsetDateTime getFrom() {
    return from;
  }

  public OffsetDateTime getTo() {
    return to;
  }

  public String getCurrency() {
    return spendDataItems.isEmpty() ? "n/a" : spendDataItems.get(0).currency();
  }
}
