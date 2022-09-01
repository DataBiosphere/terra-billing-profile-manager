package bio.terra.profile.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profile.sentry")
public record SentryConfiguration(String dsn, String environment) {}
