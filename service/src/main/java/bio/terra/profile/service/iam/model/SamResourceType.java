package bio.terra.profile.service.iam.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

public enum SamResourceType {
  PROFILE("spend-profile");

  private final String samResourceName;

  SamResourceType(String samResourceName) {
    this.samResourceName = samResourceName;
  }

  public String getSamResourceName() {
    return samResourceName;
  }

  @Override
  @JsonValue
  public String toString() {
    return samResourceName;
  }

  @JsonCreator
  static SamResourceType fromValue(String text) {
    for (SamResourceType b : SamResourceType.values()) {
      if (StringUtils.equalsIgnoreCase(b.getSamResourceName(), text)) {
        return b;
      }
    }
    return null;
  }
}
