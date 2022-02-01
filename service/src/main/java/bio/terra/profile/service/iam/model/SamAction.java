package bio.terra.profile.service.iam.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

public enum SamAction {
  CREATE("create"),
  DELETE("delete"),
  UPDATE_BILLING_ACCOUNT("update_billing_account"),
  LINK("link");

  private final String samActionName;

  SamAction(String samActionName) {
    this.samActionName = samActionName;
  }

  public String getSamActionName() {
    return samActionName;
  }

  @Override
  @JsonValue
  public String toString() {
    return samActionName;
  }

  @JsonCreator
  static SamAction fromValue(String text) {
    for (SamAction b : SamAction.values()) {
      if (StringUtils.equalsIgnoreCase(b.getSamActionName(), text)) {
        return b;
      }
    }
    return null;
  }
}
