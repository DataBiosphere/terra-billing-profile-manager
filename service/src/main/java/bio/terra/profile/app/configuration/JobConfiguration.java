package bio.terra.profile.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profile.job")
public record JobConfiguration(int maxThreads, int timeoutSeconds, int pollingIntervalSeconds) {}
