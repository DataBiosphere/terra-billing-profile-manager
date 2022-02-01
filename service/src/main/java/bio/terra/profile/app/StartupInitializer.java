package bio.terra.profile.app;

import bio.terra.common.migrate.LiquibaseMigrator;
import bio.terra.profile.app.configuration.ProfileDatabaseConfiguration;
import bio.terra.profile.service.job.JobService;
import org.springframework.context.ApplicationContext;

public final class StartupInitializer {
  private static final String changelogPath = "db/changelog.xml";

  public static void initialize(ApplicationContext applicationContext) {
    // Initialize or upgrade the database depending on the configuration
    LiquibaseMigrator migrateService = applicationContext.getBean(LiquibaseMigrator.class);
    JobService jobService = applicationContext.getBean(JobService.class);
    ProfileDatabaseConfiguration profileDatabaseConfiguration =
        applicationContext.getBean(ProfileDatabaseConfiguration.class);

    // Migrate the database
    if (profileDatabaseConfiguration.isInitializeOnStart()) {
      migrateService.initialize(changelogPath, profileDatabaseConfiguration.getDataSource());
    } else if (profileDatabaseConfiguration.isUpgradeOnStart()) {
      migrateService.upgrade(changelogPath, profileDatabaseConfiguration.getDataSource());
    }

    // The JobService initialization also handles Stairway initialization.
    jobService.initialize();

    // TODO: Fill in this method with any other initialization that needs to happen
    //  between the point of having the entire application initialized and
    //  the point of opening the port to start accepting REST requests.
  }
}
