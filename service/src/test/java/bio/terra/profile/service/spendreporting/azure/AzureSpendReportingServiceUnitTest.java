package bio.terra.profile.service.spendreporting.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.common.SpendDataFixtures;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.crl.CrlService;
import bio.terra.profile.service.profile.model.BillingProfile;
import bio.terra.profile.service.spendreporting.azure.exception.KubernetesResourceNotFound;
import bio.terra.profile.service.spendreporting.azure.exception.MultipleKubernetesResourcesFound;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import bio.terra.profile.service.spendreporting.azure.model.mapper.QueryResultMapper;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.resourcemanager.costmanagement.models.QueryResult;
import com.azure.resourcemanager.resources.ResourceManager;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.resourcemanager.resources.models.GenericResources;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class AzureSpendReportingServiceUnitTest extends BaseUnitTest {
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
  private static final String RESOURCE_GROUP_NAME = UUID.randomUUID().toString();
  private static final UUID BILLING_PROFILE_ID = UUID.randomUUID();
  private static final String K8S_RESOURCE_NAME = "k8sResource";
  private static final String REGION = "useast2";
  private static final String K8S_RESOURCE_NAME2 = "k8sResource2";

  private static final String COMPUTE_RESOURCE_TYPE = "microsoft.compute";
  private static final String STORAGE_RESOURCE_TYPE = "microsoft.storage";

  private static final Map<String, List<Object>> QUERY_RESULT_DATA =
      Map.of(
          COMPUTE_RESOURCE_TYPE,
          List.of("10.55", COMPUTE_RESOURCE_TYPE, "USD"),
          STORAGE_RESOURCE_TYPE,
          List.of("20.99", STORAGE_RESOURCE_TYPE, "USD"));

  private AzureSpendReportingService azureSpendReportingService;

  @Mock private AzureCostManagementQuery mockAzureCostManagementQuery;
  @Mock private CrlService mockCrlService;
  @Mock private QueryResultMapper mockQueryResultMapper;
  @Captor private ArgumentCaptor<UUID> subscriptionIdCaptor;
  @Captor private ArgumentCaptor<String> resourceGroupCaptor;
  @Captor private ArgumentCaptor<OffsetDateTime> fromCaptor;
  @Captor private ArgumentCaptor<OffsetDateTime> toCaptor;

  @BeforeEach
  void setup() {
    azureSpendReportingService =
        new AzureSpendReportingService(
            mockAzureCostManagementQuery, mockCrlService, mockQueryResultMapper);
  }

  @Test
  void testGetBillingProfileSpendData_K8sResourceGroupExists_Success() {
    var billingProfile =
        buildAzureBillingProfile(
            BILLING_PROFILE_ID,
            Optional.of(UUID.randomUUID().toString()),
            Optional.of(TENANT_ID),
            Optional.of(SUBSCRIPTION_ID),
            Optional.of(RESOURCE_GROUP_NAME));
    var from = OffsetDateTime.now();
    var to = from.plusDays(30);

    setupMockResponseK8sResourceGroup(
        billingProfile.getRequiredManagedResourceGroupId(),
        List.of(Pair.of(K8S_RESOURCE_NAME, REGION)),
        true);
    Response<QueryResult> response1 = mock(Response.class);
    QueryResult queryResult1 = mock(QueryResult.class);
    when(queryResult1.rows()).thenReturn(List.of(QUERY_RESULT_DATA.get(COMPUTE_RESOURCE_TYPE)));
    when(response1.getValue()).thenReturn(queryResult1);
    when(mockAzureCostManagementQuery.resourceGroupCostQueryWithResourceTypeGrouping(
            SUBSCRIPTION_ID, RESOURCE_GROUP_NAME, from, to))
        .thenReturn(response1);

    String k8sResourceGroupName =
        String.format(
            "%s_%s_%s_%s",
            AzureSpendReportingService.K8S_RESOURCE_GROUP_NAME_PREFIX,
            billingProfile.getRequiredManagedResourceGroupId(),
            K8S_RESOURCE_NAME,
            REGION);

    Response<QueryResult> response2 = mock(Response.class);
    QueryResult queryResult2 = mock(QueryResult.class);
    when(queryResult2.rows()).thenReturn(List.of(QUERY_RESULT_DATA.get(STORAGE_RESOURCE_TYPE)));
    when(response2.getValue()).thenReturn(queryResult2);
    when(mockAzureCostManagementQuery.resourceGroupCostQueryWithResourceTypeGrouping(
            SUBSCRIPTION_ID, k8sResourceGroupName, from, to))
        .thenReturn(response2);

    var spendData1 =
        SpendDataFixtures.buildSingleItemSpendData(
            COMPUTE_RESOURCE_TYPE,
            new BigDecimal("10.55"),
            "USD",
            SpendCategoryType.COMPUTE,
            from,
            to);
    when(mockQueryResultMapper.mapQueryResult(queryResult1, from, to)).thenReturn(spendData1);

    var spendData2 =
        SpendDataFixtures.buildSingleItemSpendData(
            STORAGE_RESOURCE_TYPE,
            new BigDecimal("20.99"),
            "USD",
            SpendCategoryType.STORAGE,
            from,
            to);
    when(mockQueryResultMapper.mapQueryResult(queryResult2, SpendCategoryType.COMPUTE, from, to))
        .thenReturn(spendData2);

    var spendData = azureSpendReportingService.getBillingProfileSpendData(billingProfile, from, to);

    verify(mockAzureCostManagementQuery, times(2))
        .resourceGroupCostQueryWithResourceTypeGrouping(
            subscriptionIdCaptor.capture(),
            resourceGroupCaptor.capture(),
            fromCaptor.capture(),
            toCaptor.capture());
    assertNotNull(subscriptionIdCaptor.getAllValues());
    assertEquals(2, subscriptionIdCaptor.getAllValues().size());
    assertEquals(
        2,
        subscriptionIdCaptor.getAllValues().stream()
            .filter(v -> v.equals(SUBSCRIPTION_ID))
            .count());
    assertNotNull(resourceGroupCaptor.getAllValues());
    assertEquals(2, resourceGroupCaptor.getAllValues().size());
    assertTrue(resourceGroupCaptor.getAllValues().contains(k8sResourceGroupName));
    assertTrue(resourceGroupCaptor.getAllValues().contains(RESOURCE_GROUP_NAME));
    assertNotNull(fromCaptor.getAllValues());
    assertEquals(2, fromCaptor.getAllValues().size());
    assertEquals(2, fromCaptor.getAllValues().stream().filter(v -> v.equals(from)).count());
    assertNotNull(toCaptor.getAllValues());
    assertEquals(2, toCaptor.getAllValues().size());
    assertEquals(2, toCaptor.getAllValues().stream().filter(v -> v.equals(to)).count());

    assertNotNull(spendData);
    assertNotNull(spendData.getSpendDataItems());
    assertEquals(2, spendData.getSpendDataItems().size());
    var computeItem =
        spendData.getSpendDataItems().stream()
            .filter(i -> i.spendCategoryType().equals(SpendCategoryType.COMPUTE))
            .findFirst();
    assertTrue(computeItem.isPresent());
    var storageItem =
        spendData.getSpendDataItems().stream()
            .filter(i -> i.spendCategoryType().equals(SpendCategoryType.STORAGE))
            .findFirst();
    assertTrue(storageItem.isPresent());
    assertThat(spendData.getFrom(), equalTo(from));
    assertThat(spendData.getTo(), equalTo(to));
  }

  @Test
  void testGetBillingProfileSpendData_K8sResourceDoesntExist_Failure() {
    var billingProfile =
        buildAzureBillingProfile(
            BILLING_PROFILE_ID,
            Optional.of(UUID.randomUUID().toString()),
            Optional.of(TENANT_ID),
            Optional.of(SUBSCRIPTION_ID),
            Optional.of(RESOURCE_GROUP_NAME));
    var from = OffsetDateTime.now();
    var to = from.plusDays(30);

    setupMockResponseK8sResourceGroup(
        billingProfile.getRequiredManagedResourceGroupId(), null, false);

    assertThrows(
        KubernetesResourceNotFound.class,
        () -> azureSpendReportingService.getBillingProfileSpendData(billingProfile, from, to));
  }

  @Test
  void testGetBillingProfileSpendData_MultipleK8sResourcesExists_Failure() {
    var billingProfile =
        buildAzureBillingProfile(
            BILLING_PROFILE_ID,
            Optional.of(UUID.randomUUID().toString()),
            Optional.of(TENANT_ID),
            Optional.of(SUBSCRIPTION_ID),
            Optional.of(RESOURCE_GROUP_NAME));
    var from = OffsetDateTime.now();
    var to = from.plusDays(30);

    setupMockResponseK8sResourceGroup(
        billingProfile.getRequiredManagedResourceGroupId(),
        List.of(Pair.of(K8S_RESOURCE_NAME, REGION), Pair.of(K8S_RESOURCE_NAME2, REGION)),
        true);

    assertThrows(
        MultipleKubernetesResourcesFound.class,
        () -> azureSpendReportingService.getBillingProfileSpendData(billingProfile, from, to));
  }

  private void setupMockResponseK8sResourceGroup(
      String resourceGroupName,
      /*left is name, right is region*/
      List<Pair<String, String>> pairOfK8sDetails,
      boolean responseExists) {
    ResourceManager mockResourceManager = mock(ResourceManager.class);
    when(mockCrlService.getResourceManager(any(), any())).thenReturn(mockResourceManager);

    GenericResources mockGenericResources = mock(GenericResources.class);
    PagedIterable<GenericResource> mockPagedIterable = mock(PagedIterable.class);

    if (responseExists) {
      List<GenericResource> mockK8sResources = new ArrayList<>();
      for (Pair<String, String> k8sDetails : pairOfK8sDetails) {
        GenericResource mockK8sResource = mock(GenericResource.class);
        when(mockK8sResource.name()).thenReturn(k8sDetails.getLeft());
        when(mockK8sResource.regionName()).thenReturn(k8sDetails.getRight());
        when(mockK8sResource.resourceType())
            .thenReturn(AzureSpendReportingService.AZURE_KUBERNETES_RESOURCE_TYPE);
        mockK8sResources.add(mockK8sResource);
      }
      when(mockPagedIterable.stream()).thenReturn(mockK8sResources.stream());
    } else {
      when(mockPagedIterable.stream()).thenReturn(Stream.empty());
    }

    when(mockGenericResources.listByResourceGroup(resourceGroupName)).thenReturn(mockPagedIterable);
    when(mockResourceManager.genericResources()).thenReturn(mockGenericResources);
  }

  private BillingProfile buildAzureBillingProfile(
      UUID id,
      Optional<String> billingAccountId,
      Optional<UUID> tenantId,
      Optional<UUID> subscriptionId,
      Optional<String> managedResourceGroup) {
    return new BillingProfile(
        id,
        "Test billing profile",
        "Test billing profile",
        "biller",
        CloudPlatform.AZURE,
        billingAccountId,
        tenantId,
        subscriptionId,
        managedResourceGroup,
        Instant.now(),
        Instant.now(),
        "testUser");
  }
}