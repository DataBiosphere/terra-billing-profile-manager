package bio.terra.profile.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profile.sam")
public record SamConfiguration(String basePath, String adminsGroupEmail) {}
