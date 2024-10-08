package bio.terra.profile.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.profile.exception.DuplicateManagedApplicationException;
import bio.terra.profile.service.profile.exception.MissingRequiredFieldsException;
import bio.terra.profile.service.profile.exception.ProfileInUseException;
import bio.terra.profile.service.profile.exception.ProfileNotFoundException;
import bio.terra.profile.service.profile.model.BillingProfile;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
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
          + "tenant_id, subscription_id, managed_resource_group_id, "
          + "created_date, created_by, last_modified";

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
  public BillingProfile createBillingProfile(BillingProfile profile, String initiatingUser) {
    String sql =
        "INSERT INTO billing_profile"
            + " (id, display_name, biller, billing_account_id, description, cloud_platform, "
            + "     tenant_id, subscription_id, managed_resource_group_id, created_by) VALUES "
            + " (:id, :display_name, :biller, :billing_account_id, :description, :cloud_platform, "
            + "     :tenant_id, :subscription_id, :managed_resource_group_id, :created_by)";

    String billingAccountId = profile.billingAccountId().orElse(null);
    UUID tenantId = profile.tenantId().orElse(null);
    UUID subscriptionId = profile.subscriptionId().orElse(null);
    String managedResourceGroupId = profile.managedResourceGroupId().orElse(null);

    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("id", profile.id())
            .addValue("display_name", profile.displayName())
            .addValue("biller", profile.biller())
            .addValue("billing_account_id", billingAccountId)
            .addValue("description", profile.description())
            .addValue("cloud_platform", profile.cloudPlatform().name())
            .addValue("tenant_id", tenantId)
            .addValue("subscription_id", subscriptionId)
            .addValue("managed_resource_group_id", managedResourceGroupId)
            .addValue("created_by", initiatingUser);

    var keyHolder = new DaoKeyHolder();
    try {
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
          profile.managedResourceGroupId(),
          keyHolder.getInstant("created_date"),
          keyHolder.getInstant("last_modified"),
          keyHolder.getString("created_by"));
    } catch (DuplicateKeyException ex) {
      if (ex.getMessage() != null
          && ex.getMessage()
              .contains("billing_profile_subscription_id_managed_resource_group_id_key")) {
        throw new DuplicateManagedApplicationException("Managed application already in use.");
      } else {
        throw ex;
      }
    }
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

  public List<String> listManagedResourceGroupsInSubscription(UUID subscriptionId) {
    var params = new MapSqlParameterSource().addValue("subscriptionId", subscriptionId);
    return jdbcTemplate.queryForList(
        "SELECT managed_resource_group_id from billing_profile"
            + " where subscription_id = :subscriptionId",
        params,
        String.class);
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

  @WriteTransaction
  public boolean updateProfile(
      UUID id, @Nullable String description, @Nullable String billingAccountId) {
    if (description == null && billingAccountId == null) {
      throw new MissingRequiredFieldsException("Must specify field to update.");
    }

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("id", id);
    ArrayList<String> setClause = new ArrayList<>();

    if (description != null) {
      params.addValue("description", description);
      setClause.add("description = :description");
    }

    if (billingAccountId != null) {
      params.addValue("billing_account_id", billingAccountId);
      setClause.add("billing_account_id = :billing_account_id");
    }

    setClause.add("last_modified = current_timestamp(6)");

    String sql =
        String.format("UPDATE billing_profile SET %s WHERE id = :id", String.join(",", setClause));

    return jdbcTemplate.update(sql, params) > 0;
  }

  @WriteTransaction
  public boolean removeBillingAccount(UUID id) {
    var sql =
        "UPDATE billing_profile SET billing_account_id = null, last_modified = current_timestamp(6) where id = :id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    return jdbcTemplate.update(sql, params) > 0;
  }

  private static class BillingProfileMapper implements RowMapper<BillingProfile> {

    public BillingProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new BillingProfile(
          rs.getObject("id", UUID.class),
          rs.getString("display_name"),
          rs.getString("description"),
          rs.getString("biller"),
          CloudPlatform.fromValue(rs.getString("cloud_platform")),
          Optional.ofNullable(rs.getString("billing_account_id")),
          Optional.ofNullable(rs.getObject("tenant_id", UUID.class)),
          Optional.ofNullable(rs.getObject("subscription_id", UUID.class)),
          Optional.ofNullable(rs.getString("managed_resource_group_id")),
          rs.getTimestamp("created_date").toInstant(),
          rs.getTimestamp("last_modified").toInstant(),
          rs.getString("created_by"));
    }
  }
}
