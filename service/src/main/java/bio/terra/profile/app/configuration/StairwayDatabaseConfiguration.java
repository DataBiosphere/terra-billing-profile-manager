package bio.terra.profile.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "profile.stairway-database")
public class StairwayDatabaseConfiguration extends BaseDatabaseConfiguration {
  /** Passed to Stairway, true will run the migrate to upgrade the database */
  private boolean migrateUpgrade;

  /**
   * Passed to Stairway, true will drop any existing stairway data and purge the work queue.
   * Otherwise existing flights are recovered.
   */
  private boolean forceClean;

  public void setMigrateUpgrade(boolean migrateUpgrade) {
    this.migrateUpgrade = migrateUpgrade;
  }

  public void setForceClean(boolean forceClean) {
    this.forceClean = forceClean;
  }

  public boolean isMigrateUpgrade() {
    return migrateUpgrade;
  }

  public boolean isForceClean() {
    return forceClean;
  }
}
