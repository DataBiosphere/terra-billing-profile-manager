package bio.terra.profile.app.configuration;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "profile.limits")
public class LimitsConfiguration {
    private Map<UUID, Map<String, String>> profiles;

    public Map<UUID, Map<String, String>> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<UUID, Map<String, String>> profiles) {
        this.profiles = profiles;
    }

    public Map<String, String> getLimitsForProfile(UUID profileId){
        return this.profiles.getOrDefault(profileId, Collections.emptyMap());
    }
}


