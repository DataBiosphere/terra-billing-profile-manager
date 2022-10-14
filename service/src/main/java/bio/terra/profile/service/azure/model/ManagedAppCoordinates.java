package bio.terra.profile.service.azure.model;

import java.util.UUID;

public record ManagedAppCoordinates(
        UUID tenantId,
        UUID subscriptionId
) {
}
