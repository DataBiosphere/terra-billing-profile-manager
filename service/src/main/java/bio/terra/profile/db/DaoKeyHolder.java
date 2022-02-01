package bio.terra.profile.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.support.GeneratedKeyHolder;

public class DaoKeyHolder extends GeneratedKeyHolder {
  public Instant getCreatedDate() {
    return getField("created_date", Timestamp.class).map(Timestamp::toInstant).orElse(null);
  }

  public String getString(String fieldName) {
    return getField(fieldName, String.class).orElse(null);
  }

  public <T> Optional<T> getField(String fieldName, Class<T> type) {
    Map<String, Object> keys = getKeys();
    if (keys != null) {
      Object fieldObject = keys.get(fieldName);
      if (type.isInstance(fieldObject)) {
        return Optional.of(type.cast(fieldObject));
      }
    }
    return Optional.empty();
  }
}
