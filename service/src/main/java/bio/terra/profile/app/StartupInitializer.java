package bio.terra.profile.app;

import bio.terra.common.migrate.LiquibaseMigrator;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.app.configuration.ProfileDatabaseConfiguration;
import bio.terra.profile.app.configuration.SentryConfiguration;
import bio.terra.profile.service.job.JobService;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public final class StartupInitializer {
  private static final String changelogPath = "db/changelog.xml";
  private static final Logger logger = LoggerFactory.getLogger(StartupInitializer.class);

  public static void initialize(ApplicationContext applicationContext) {
    // Initialize or upgrade the database depending on the configuration
    LiquibaseMigrator migrateService = applicationContext.getBean(LiquibaseMigrator.class);
    JobService jobService = applicationContext.getBean(JobService.class);
    ProfileDatabaseConfiguration profileDatabaseConfiguration =
        applicationContext.getBean(ProfileDatabaseConfiguration.class);
    SentryConfiguration sentryConfiguration = applicationContext.getBean(SentryConfiguration.class);
    AzureConfiguration azureConfiguration = applicationContext.getBean(AzureConfiguration.class);
    azureConfiguration.logOffers();

    // Migrate the database
    if (profileDatabaseConfiguration.isInitializeOnStart()) {
      migrateService.initialize(changelogPath, profileDatabaseConfiguration.getDataSource());
    } else if (profileDatabaseConfiguration.isUpgradeOnStart()) {
      migrateService.upgrade(changelogPath, profileDatabaseConfiguration.getDataSource());
    }

    // The JobService initialization also handles Stairway initialization.
    jobService.initialize();

    if (sentryConfiguration.dsn().isEmpty()) {
      logger.info("No Sentry DSN found. Starting up without it.");
    } else {
      logger.info("Sentry DSN found. 5xx errors will be sent to Sentry.");
      Sentry.init(
          options -> {
            options.setDsn(sentryConfiguration.dsn());
            options.setEnvironment(sentryConfiguration.environment());
          });
    }
    // TODO: Fill in this method with any other initialization that needs to happen
    //  between the point of having the entire application initialized and
    //  the point of opening the port to start accepting REST requests.
  }
}
