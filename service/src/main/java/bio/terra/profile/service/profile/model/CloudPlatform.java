package bio.terra.profile.service.profile.model;

import bio.terra.profile.generated.model.ApiCloudPlatform;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.StringUtils;

public enum CloudPlatform {
  GCP("GCP", ApiCloudPlatform.GCP),
  AZURE("AZURE", ApiCloudPlatform.AZURE);

  private final String dbString;
  private final ApiCloudPlatform apiCloudPlatform;

  CloudPlatform(String dbString, ApiCloudPlatform apiCloudPlatform) {
    this.dbString = dbString;
    this.apiCloudPlatform = apiCloudPlatform;
  }

  public String toSql() {
    return dbString;
  }

  public ApiCloudPlatform toApi() {
    return apiCloudPlatform;
  }

  public static CloudPlatform fromSql(String dbString) {
    for (CloudPlatform value : values()) {
      if (StringUtils.equals(value.dbString, dbString)) {
        return value;
      }
    }
    throw new SerializationException(
        "Deserialization failed: no matching cloud platform for " + dbString);
  }

  public static CloudPlatform fromApi(ApiCloudPlatform apiCloudPlatform) {
    for (CloudPlatform value : values()) {
      if (value.apiCloudPlatform.equals(apiCloudPlatform)) {
        return value;
      }
    }
    throw new SerializationException(
        "Deserialization failed: no matching cloud platform for " + apiCloudPlatform);
  }
}
