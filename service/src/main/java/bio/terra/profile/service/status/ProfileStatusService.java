package bio.terra.profile.service.status;

import bio.terra.profile.app.configuration.StatusCheckConfiguration;
import bio.terra.profile.generated.model.ApiSystemStatusSystems;
import java.sql.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProfileStatusService extends BaseStatusService {
  private static final Logger logger = LoggerFactory.getLogger(ProfileStatusService.class);
  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public ProfileStatusService(
      NamedParameterJdbcTemplate jdbcTemplate, StatusCheckConfiguration configuration) {
    super(configuration);
    this.jdbcTemplate = jdbcTemplate;
    super.registerStatusCheck("CloudSQL", this::databaseStatus);
  }

  private ApiSystemStatusSystems databaseStatus() {
    try {
      logger.debug("Checking database connection valid");
      return new ApiSystemStatusSystems()
          .ok(jdbcTemplate.getJdbcTemplate().execute((Connection conn) -> conn.isValid(5000)));
    } catch (Exception ex) {
      String errorMsg = "Database status check failed";
      logger.error(errorMsg, ex);
      return new ApiSystemStatusSystems()
          .ok(false)
          .addMessagesItem(errorMsg + ": " + ex.getMessage());
    }
  }
}
