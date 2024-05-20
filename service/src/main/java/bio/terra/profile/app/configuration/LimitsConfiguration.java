package bio.terra.profile.app.configuration;

import java.util.Map;
import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profile.limits")
public record LimitsConfiguration(Map<UUID, Map<String, String>> subscriptions) {}
