package bio.terra.profile.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.service.profile.exception.ProfileInUseException;
import bio.terra.profile.service.profile.exception.ProfileNotFoundException;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.profile.model.CloudPlatform;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileDao {
  private final NamedParameterJdbcTemplate jdbcTemplate;

  // SQL select string constants
  private static final String SQL_SELECT_LIST =
      "id, display_name, biller, billing_account_id, description, cloud_platform, "
          + "tenant_id, subscription_id, resource_group_name, application_deployment_name, created_date, created_by";

  private static final String SQL_GET =
      "SELECT " + SQL_SELECT_LIST + " FROM billing_profile WHERE id = :id";

  private static final String SQL_LIST =
      "SELECT "
          + SQL_SELECT_LIST
          + " FROM billing_profile"
          + " WHERE id in (:profile_ids)"
          + " OFFSET :offset LIMIT :limit";

  @Autowired
  public ProfileDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @WriteTransaction
  public BillingProfile createBillingProfile(
      BillingProfile profile, AuthenticatedUserRequest user) {
    String sql =
        "INSERT INTO billing_profile"
            + " (id, display_name, biller, billing_account_id, description, cloud_platform, "
            + "     tenant_id, subscription_id, resource_group_name, application_deployment_name, created_by) VALUES "
            + " (:id, :display_name, :biller, :billing_account_id, :description, :cloud_platform, "
            + "     :tenant_id, :subscription_id, :resource_group_name, :application_deployment_name, :created_by)";

    String billingAccountId = profile.billingAccountId().orElse(null);
    String tenantId = profile.tenantId().map(UUID::toString).orElse(null);
    String subscriptionId = profile.subscriptionId().map(UUID::toString).orElse(null);
    String resourceGroupName = profile.resourceGroupName().orElse(null);
    String applicationDeploymentName = profile.applicationDeploymentName().orElse(null);

    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("id", profile.id())
            .addValue("display_name", profile.displayName())
            .addValue("biller", profile.biller())
            .addValue("billing_account_id", billingAccountId)
            .addValue("description", profile.description())
            .addValue("cloud_platform", profile.cloudPlatform().toSql())
            .addValue("tenant_id", tenantId)
            .addValue("subscription_id", subscriptionId)
            .addValue("resource_group_name", resourceGroupName)
            .addValue("application_deployment_name", applicationDeploymentName)
            .addValue("created_by", user.getEmail());

    var keyHolder = new DaoKeyHolder();
    jdbcTemplate.update(sql, params, keyHolder);

    return new BillingProfile(
        profile.id(),
        profile.displayName(),
        profile.description(),
        profile.biller(),
        profile.cloudPlatform(),
        profile.billingAccountId(),
        profile.tenantId(),
        profile.subscriptionId(),
        profile.resourceGroupName(),
        profile.applicationDeploymentName(),
        keyHolder.getCreatedDate(),
        keyHolder.getString("created_by"));
  }

  @ReadTransaction
  public List<BillingProfile> listBillingProfiles(int offset, int limit, Collection<UUID> idList) {

    // If the incoming list is empty, the caller does not have permission to see any
    // workspaces, so we return an empty list.
    if (idList.isEmpty()) {
      return Collections.emptyList();
    }
    var params =
        new MapSqlParameterSource()
            .addValue("profile_ids", idList)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return jdbcTemplate.query(SQL_LIST, params, new BillingProfileMapper());
  }

  @ReadTransaction
  public BillingProfile getBillingProfileById(UUID id) {
    try {
      MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
      return jdbcTemplate.queryForObject(SQL_GET, params, new BillingProfileMapper());
    } catch (EmptyResultDataAccessException ex) {
      throw new ProfileNotFoundException("Profile not found for id: " + id.toString());
    }
  }

  @WriteTransaction
  public boolean deleteBillingProfileById(UUID id) {
    try {
      int rowsAffected =
          jdbcTemplate.update(
              "DELETE FROM billing_profile WHERE id = :id",
              new MapSqlParameterSource().addValue("id", id));
      return rowsAffected > 0;
    } catch (DataIntegrityViolationException ex) {
      // Just in case some concurrent thing slips through the usage check step,
      // handle a case of some active references.
      throw new ProfileInUseException("Profile is in use and cannot be deleted", ex);
    }
  }

  private static class BillingProfileMapper implements RowMapper<BillingProfile> {
    public BillingProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new BillingProfile(
          rs.getObject("id", UUID.class),
          rs.getString("display_name"),
          rs.getString("description"),
          rs.getString("biller"),
          CloudPlatform.fromSql(rs.getString("cloud_platform")),
          Optional.ofNullable(rs.getString("billing_account_id")),
          Optional.ofNullable(rs.getObject("tenant_id", UUID.class)),
          Optional.ofNullable(rs.getObject("subscription_id", UUID.class)),
          Optional.ofNullable(rs.getString("resource_group_name")),
          Optional.ofNullable(rs.getString("application_deployment_name")),
          rs.getTimestamp("created_date").toInstant(),
          rs.getString("created_by"));
    }
  }
}
