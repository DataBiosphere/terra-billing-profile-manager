package bio.terra.profile.service.spendreporting.azure.model.mapper;

import static bio.terra.profile.service.spendreporting.azure.model.mapper.SpendDataItemCategoryMapper.AZURE_COMPUTE_RESOURCE_TYPE;
import static bio.terra.profile.service.spendreporting.azure.model.mapper.SpendDataItemCategoryMapper.AZURE_STORAGE_RESOURCE_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import bio.terra.profile.common.BaseUnitTest;
import bio.terra.profile.service.spendreporting.azure.model.SpendCategoryType;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SpendDataItemCategoryMapperUnitTest extends BaseUnitTest {
  private SpendDataItemCategoryMapper spendDataItemCategoryMapper;

  @BeforeEach
  void setup() {
    spendDataItemCategoryMapper = new SpendDataItemCategoryMapper();
  }

  @Test
  void testCompute() {
    assertThat(
        spendDataItemCategoryMapper.mapResourceCategory(AZURE_COMPUTE_RESOURCE_TYPE),
        equalTo(SpendCategoryType.COMPUTE));
  }

  @Test
  void testStorage() {
    assertThat(
        spendDataItemCategoryMapper.mapResourceCategory(AZURE_STORAGE_RESOURCE_TYPE),
        equalTo(SpendCategoryType.STORAGE));
  }

  @ParameterizedTest
  @MethodSource("getOtherAzureResourceTypes")
  void testOther(String azureResourceTypes) {
    assertThat(
        spendDataItemCategoryMapper.mapResourceCategory(azureResourceTypes),
        equalTo(SpendCategoryType.OTHER));
  }

  private static Stream<Arguments> getOtherAzureResourceTypes() {
    return Stream.of(
        Arguments.of("microsoft.network"),
        Arguments.of("microsoft.relay"),
        Arguments.of("microsoft.dbforpostgresql"),
        Arguments.of("microsoft.operationalinsights"));
  }
}
