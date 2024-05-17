package bio.terra.profile.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ConfigurationProperties(prefix = "profile.limits")
public record LimitsConfiguration(Map<UUID, Map<String,String>> limits) {
}
