package bio.terra.profile.service.spendreporting.azure.model.mapper;

import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SpendDataItemCategoryMapper {
  public static final String AZURE_COMPUTE_RESOURCE_TYPE = "microsoft.compute";
  public static final String AZURE_STORAGE_RESOURCE_TYPE = "microsoft.storage";

  public SpendCategoryType mapResourceCategory(String resourceCategory) {
    if (resourceCategory
        .toLowerCase(Locale.ROOT)
        .startsWith(AZURE_COMPUTE_RESOURCE_TYPE.toLowerCase(Locale.ROOT))) {
      return SpendCategoryType.COMPUTE;
    } else if (resourceCategory
        .toLowerCase(Locale.ROOT)
        .startsWith(AZURE_STORAGE_RESOURCE_TYPE.toLowerCase(Locale.ROOT))) {
      return SpendCategoryType.STORAGE;
    } else {
      return SpendCategoryType.OTHER;
    }
  }
}
