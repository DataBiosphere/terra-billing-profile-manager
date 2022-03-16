package bio.terra.profile.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "profile.ingress")
public record IngressConfiguration(String domainName) {}
