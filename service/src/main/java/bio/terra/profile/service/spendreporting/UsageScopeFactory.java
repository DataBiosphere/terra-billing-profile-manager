package bio.terra.profile.service.spendreporting;

public class UsageScopeFactory {
  public static String buildResourceGroupUsageScope(
      String subscriptionId, String resourceGroupName) {
    return String.format("subscriptions/%s/resourceGroups/%s", subscriptionId, resourceGroupName);
  }
}
