package bio.terra.profile.service.spendreporting.model;

import java.util.Collections;
import java.util.List;

public class SpendData {
  private final List<SpendDataItem> spendDataItems;

  public SpendData(List<SpendDataItem> spendDataItems) {
    this.spendDataItems = spendDataItems;
  }

  public List<SpendDataItem> getCostItems() {
    return Collections.unmodifiableList(spendDataItems);
  }
}
