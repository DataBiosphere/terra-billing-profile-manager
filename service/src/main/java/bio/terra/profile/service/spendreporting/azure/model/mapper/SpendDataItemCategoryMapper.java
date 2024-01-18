package bio.terra.profile.service.spendreporting.azure.model.mapper;

import bio.terra.profile.service.spendreporting.azure.model.AzureResourceProviderType;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SpendDataItemCategoryMapper {
  public static final Set<AzureResourceProviderType> AZURE_COMPUTE_RESOURCE_TYPES =
      Set.of(AzureResourceProviderType.COMPUTE, AzureResourceProviderType.BATCH);
  public static final Set<AzureResourceProviderType> AZURE_STORAGE_RESOURCE_TYPES =
      Set.of(AzureResourceProviderType.STORAGE);

  public SpendCategoryType mapResourceCategory(String resourceCategory) {
    if (AZURE_COMPUTE_RESOURCE_TYPES.contains(
        AzureResourceProviderType.fromString(resourceCategory))) {
      return SpendCategoryType.COMPUTE;
    } else if (AZURE_STORAGE_RESOURCE_TYPES.contains(
        AzureResourceProviderType.fromString(resourceCategory))) {
      return SpendCategoryType.STORAGE;
    } else {
      return SpendCategoryType.OTHER;
    }
  }
}
