package bio.terra.profile.pact.provider;


import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import bio.terra.profile.app.Main;
import bio.terra.profile.app.configuration.AzureConfiguration;
import bio.terra.profile.app.configuration.BeanConfig;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.service.crl.AzureCrlService;
import bio.terra.profile.service.crl.GcpCrlService;
import bio.terra.profile.service.iam.SamService;
import bio.terra.profile.service.iam.model.SamResourceType;
import com.azure.core.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Tag("provider-test")
@Provider("bpm-provider")
@PactFolder("pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Main.class)
@SpringBootApplication(
    scanBasePackages = {
        // Dependencies for Stairway
        "bio.terra.common.kubernetes",
        // Scan for logging-related components & configs
        "bio.terra.common.logging",
        // Scan for Liquibase migration components & configs
        "bio.terra.common.migrate",
        // Transaction management and DB retry configuration
        "bio.terra.common.retry.transaction",
        // Stairway initialization and status
        "bio.terra.common.stairway",
        // Scan for tracing-related components & configs
        "bio.terra.common.tracing",
        // Scan all service-specific packages beneath the current package
        "bio.terra.profile"
    },
    exclude = {
        DataSourceAutoConfiguration.class,
    })
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {

        AzureCrlService.class,
        GcpCrlService.class,
        SamService.class,
        BeanConfig.class,
    })})
@ConfigurationPropertiesScan(basePackages = {"bio.terra.profile"})
@ServletComponentScan(basePackages = {"bio.terra.profile"})
@EnableRetry
@EnableTransactionManagement
@EnableScheduling
public class BPMProviderTest {

  @LocalServerPort
  int port;

  @MockBean
  AzureCrlService azureCrlService;
  @MockBean
  GcpCrlService gcpCrlService;
  @MockBean
  ProfileDao profileDao;
  @MockBean
  SamService samService;

  @BeforeEach
  void setUp(PactVerificationContext context) {
    context.setTarget(new HttpTestTarget("localhost", port));
    context.setExecutionContext(ProviderStateData.providerStateValues);
  }


  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
    //PactVerificationSpringProvider
  void verifyPact(PactVerificationContext context) {
    //, HttpRequest request
    context.verifyInteraction();
  }


  @State("a GCP billing profile")
  void gcpBillingProfileState() throws InterruptedException {
    var profile = ProviderStateData.gcpBillingProfile;
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);
    when(samService.hasActions(any(), eq(SamResourceType.PROFILE), eq(profile.id()))).thenReturn(true);
  }

  @State("an Azure billing profile")
  void azureBillingProfileState() throws InterruptedException {
    var profile = ProviderStateData.azureBillingProfile;
    when(profileDao.getBillingProfileById(profile.id())).thenReturn(profile);
    when(samService.hasActions(any(), eq(SamResourceType.PROFILE), eq(profile.id()))).thenReturn(true);

  }

}
