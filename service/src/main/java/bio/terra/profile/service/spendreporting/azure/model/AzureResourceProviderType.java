package bio.terra.profile.service.spendreporting.azure.model;

import java.util.Optional;
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

  public static Optional<AzureResourceProviderType> fromString(String value) {
    for (AzureResourceProviderType type : AzureResourceProviderType.values()) {
      if (StringUtils.equalsIgnoreCase(type.getValue(), value)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }
}
