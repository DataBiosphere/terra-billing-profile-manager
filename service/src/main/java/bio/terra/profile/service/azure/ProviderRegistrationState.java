package bio.terra.profile.service.azure;

public enum ProviderRegistrationState {
  REGISTERED("Registered"),
  REGISTERING("Registering"),
  NOT_REGISTERED("NotRegistered");

  private final String providerRegistrationState;

  ProviderRegistrationState(String providerRegistrationState) {
    this.providerRegistrationState = providerRegistrationState;
  }

  public String getProviderRegistrationState() {
    return this.providerRegistrationState;
  }

  public static ProviderRegistrationState fromValue(String value) {
    if (value == null) {
      return null;
    }

    for (ProviderRegistrationState state : values()) {
      if (state.providerRegistrationState.equals(value)) {
        return state;
      }
    }

    throw new RuntimeException("ProviderRegistrationState value not found: " + value);
  }
}
