package bio.terra.profile.service.spendreporting.azure;

import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import org.springframework.stereotype.Component;

@Component
public class SpendDataItemCategoryMapper {
  public static final String AZURE_COMPUTE_RESOURCE_TYPE = "microsoft.compute";
  public static final String AZURE_STORAGE_RESOURCE_TYPE = "microsoft.storage";

  public SpendCategoryType mapResourceCategory(String resourceCategory) {
    if (resourceCategory.startsWith(AZURE_COMPUTE_RESOURCE_TYPE)) {
      return SpendCategoryType.COMPUTE;
    } else if (resourceCategory.startsWith(AZURE_STORAGE_RESOURCE_TYPE)) {
      return SpendCategoryType.STORAGE;
    } else {
      return SpendCategoryType.OTHER;
    }
  }
}
