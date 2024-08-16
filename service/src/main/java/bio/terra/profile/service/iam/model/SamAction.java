package bio.terra.profile.service.iam.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

public enum SamAction {
  CREATE("create"),
  DELETE("delete"),
  UPDATE_BILLING_ACCOUNT("update_billing_account"),
  UPDATE_METADATA("update_metadata"),
  LINK("link"),
  READ_SPEND_REPORT("read_spend_report"),
  READ_PROFILE("read_profile"),
  SPECIFY_ACTING_USER("admin_specify_acting_user");

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
