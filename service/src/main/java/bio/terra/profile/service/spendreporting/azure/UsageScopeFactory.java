package bio.terra.profile.service.spendreporting.azure;

public class UsageScopeFactory {
  private UsageScopeFactory() {}

  public static String buildResourceGroupUsageScope(
      String subscriptionId, String resourceGroupName) {
    return String.format("subscriptions/%s/resourceGroups/%s", subscriptionId, resourceGroupName);
  }
}
