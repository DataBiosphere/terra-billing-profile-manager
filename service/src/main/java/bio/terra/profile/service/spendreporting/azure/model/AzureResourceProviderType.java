package bio.terra.profile.service.spendreporting.azure.model;

import org.apache.commons.lang3.StringUtils;

public enum AzureResourceProviderType {
  COMPUTE("microsoft.compute"),
  BATCH("microsoft.batch"),
  STORAGE("microsoft.storage");

  private String value;

  AzureResourceProviderType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static AzureResourceProviderType fromString(String value) {
    for (AzureResourceProviderType type : AzureResourceProviderType.values()) {
      if (StringUtils.equalsIgnoreCase(type.getValue(), value)) {
        return type;
      }
    }
    return null;
  }
}
