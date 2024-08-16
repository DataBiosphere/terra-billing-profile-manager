package bio.terra.profile.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.SerializationException;
import bio.terra.profile.model.ChangeLogEntry;
import bio.terra.profile.service.profile.model.BillingProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Provides data access layer to record changes to billing profiles. Records creations, deletions
 * and updates. In the case of updates, also records a map that is serialized to json, describing
 * the changes
 */
@Repository
public class ProfileChangeLogDao {
  private static final Logger logger = LoggerFactory.getLogger(ProfileChangeLogDao.class);

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public static final String CHANGELOG_TABLE = "billing_profile_changelog";

  public static final String ID = "id";
  public static final String PROFILE_ID = "profile_id";
  public static final String CHANGES = "changes";
  public static final String CHANGE_DATE = "change_date";
  public static final String CHANGE_BY = "change_by";
  public static final String CHANGE_TYPE = "change_type";

  private static final List<String> COLUMNS =
      List.of(ID, PROFILE_ID, CHANGE_DATE, CHANGE_TYPE, CHANGE_BY, CHANGES);
  private static final String SQL_SELECT_LIST = String.join(", ", COLUMNS);

  private static final String INSERT_INTO_TABLE = "INSERT INTO " + CHANGELOG_TABLE;
  private static final String SQL_GET_BY_PROFILE_ID =
      "SELECT %s FROM %s WHERE %s = :%s ORDER BY %s"
          .formatted(SQL_SELECT_LIST, CHANGELOG_TABLE, PROFILE_ID, PROFILE_ID, CHANGE_DATE);

  @Autowired
  public ProfileChangeLogDao(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
  }

  @ReadTransaction
  public List<ChangeLogEntry> getChangesByProfile(UUID profileId) {
    var params = new MapSqlParameterSource().addValue(PROFILE_ID, profileId);
    return jdbcTemplate.query(SQL_GET_BY_PROFILE_ID, params, new ChangeLogMapper(objectMapper));
  }

  @WriteTransaction
  public Optional<UUID> recordProfileCreate(BillingProfile profile, String userId) {
    var sql =
        INSERT_INTO_TABLE
            + " (profile_id, change_type, change_by, change_date) VALUES (:profile_id, :change_type, :change_by, :change_date)";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(PROFILE_ID, profile.id())
            .addValue(CHANGE_TYPE, ChangeLogEntry.ChangeTypeEnum.CREATE.name())
            .addValue(CHANGE_DATE, profile.createdTime().atOffset(ZoneOffset.UTC))
            .addValue(CHANGE_BY, userId);

    var keyHolder = new DaoKeyHolder();
    jdbcTemplate.update(sql, params, keyHolder);
    return keyHolder.getField(ID, UUID.class);
  }

  @WriteTransaction
  public Optional<UUID> recordProfileUpdate(UUID profileId, String userId, Map<?, ?> changes) {
    var sql =
        INSERT_INTO_TABLE
            + " (profile_id, change_type, change_by, changes) VALUES "
            + "(:profile_id, :change_type, :change_by, :changes::jsonb)";

    String serializedChanges = null;
    try {
      serializedChanges = objectMapper.writeValueAsString(changes);
    } catch (JsonProcessingException e) {
      logger.error(
          "Unable to serialize billing profile changelog entry for {} update: {}",
          profileId,
          changes,
          e);
    }
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(PROFILE_ID, profileId)
            .addValue(CHANGE_TYPE, ChangeLogEntry.ChangeTypeEnum.UPDATE.name())
            .addValue(CHANGE_BY, userId)
            .addValue(CHANGES, serializedChanges);
    var keyHolder = new DaoKeyHolder();
    jdbcTemplate.update(sql, params, keyHolder);
    return keyHolder.getField(ID, UUID.class);
  }

  @WriteTransaction
  public Optional<UUID> recordProfileDelete(UUID profileId, String userId) {
    var sql =
        INSERT_INTO_TABLE
            + " (profile_id, change_type, change_by) VALUES (:profile_id, :change_type, :change_by)";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(PROFILE_ID, profileId)
            .addValue(CHANGE_TYPE, ChangeLogEntry.ChangeTypeEnum.DELETE.name())
            .addValue(CHANGE_BY, userId);
    var keyHolder = new DaoKeyHolder();
    jdbcTemplate.update(sql, params, keyHolder);
    return keyHolder.getField(ID, UUID.class);
  }

  static class ChangeLogMapper implements RowMapper<ChangeLogEntry> {

    private final ObjectMapper objectMapper;

    public ChangeLogMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    Map<Object, Object> getChanges(ResultSet rs) throws SQLException {
      String rawChanges = rs.getString(CHANGES);
      if (rawChanges != null) {
        try {
          return objectMapper.readValue(rawChanges, Map.class);
        } catch (JsonProcessingException e) {
          throw new SerializationException("Unable to deserialize changes in changelog entry", e);
        }
      } else {
        return Map.of();
      }
    }

    @Override
    public ChangeLogEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new ChangeLogEntry()
          .id(rs.getObject(ID, UUID.class))
          .profileId(rs.getObject(PROFILE_ID, UUID.class))
          .changeBy(rs.getString(CHANGE_BY))
          .changeType(ChangeLogEntry.ChangeTypeEnum.fromValue(rs.getString(CHANGE_TYPE)))
          .changeDate(rs.getTimestamp(CHANGE_DATE))
          .changes(getChanges(rs));
    }
  }
}
