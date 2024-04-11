package bio.terra.profile.app.configuration;

import java.util.Set;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profile.enterprise")
public record EnterpriseConfiguration(Set<UUID> subscriptions) {}
