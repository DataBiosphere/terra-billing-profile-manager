package bio.terra.profile.service.iam.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

public enum SamRole {
  ADMIN("admin"),
  OWNER("owner"),
  USER("user");

  private final String samRoleName;

  SamRole(String samRoleName) {
    this.samRoleName = samRoleName;
  }

  public String getSamRoleName() {
    return samRoleName;
  }

  @Override
  @JsonValue
  public String toString() {
    return samRoleName;
  }

  @JsonCreator
  static SamRole fromValue(String text) {
    for (SamRole b : SamRole.values()) {
      if (StringUtils.equalsIgnoreCase(b.getSamRoleName(), text)) {
        return b;
      }
    }
    return null;
  }
}
