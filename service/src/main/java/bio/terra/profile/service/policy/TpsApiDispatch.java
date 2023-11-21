package bio.terra.profile.service.policy;

import bio.terra.common.logging.RequestIdFilter;
import bio.terra.policy.api.TpsApi;
import bio.terra.policy.client.ApiClient;
import bio.terra.policy.client.ApiException;
import bio.terra.policy.model.TpsComponent;
import bio.terra.policy.model.TpsObjectType;
import bio.terra.policy.model.TpsPaoCreateRequest;
import bio.terra.policy.model.TpsPaoGetResult;
import bio.terra.policy.model.TpsPolicyInputs;
import bio.terra.profile.app.configuration.PolicyServiceConfiguration;
import bio.terra.profile.service.policy.exception.PolicyConflictException;
import bio.terra.profile.service.policy.exception.PolicyServiceAPIException;
import bio.terra.profile.service.policy.exception.PolicyServiceAuthorizationException;
import bio.terra.profile.service.policy.exception.PolicyServiceDuplicateException;
import bio.terra.profile.service.policy.exception.PolicyServiceNotFoundException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.io.IOException;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TpsApiDispatch {

  PolicyServiceConfiguration policyServiceConfiguration;
  private static final Logger logger = LoggerFactory.getLogger(TpsApiDispatch.class);

  @Autowired
  TpsApiDispatch(PolicyServiceConfiguration policyServiceConfiguration) {
    this.policyServiceConfiguration = policyServiceConfiguration;
  }

  private ApiClient getApiClient(String accessToken) {
    ApiClient client =
        new ApiClient()
            .setBasePath(policyServiceConfiguration.getBasePath())
            .addDefaultHeader(
                RequestIdFilter.REQUEST_ID_HEADER, MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    client.setAccessToken(accessToken);
    return client;
  }

  private TpsApi policyApi() {
    try {
      return new TpsApi(
          getApiClient(policyServiceConfiguration.getAccessToken())
              .setBasePath(policyServiceConfiguration.getBasePath()));
    } catch (IOException e) {
      throw new PolicyServiceAuthorizationException(
          "Error reading or parsing credentials file at %s"
              .formatted(policyServiceConfiguration.getClientCredentialFilePath()),
          e.getCause());
    }
  }

  // -- Policy Attribute Object Interface --
  @WithSpan
  public void createPao(
      UUID objectId, TpsPolicyInputs inputs, TpsComponent component, TpsObjectType objectType)
      throws InterruptedException {
    TpsApi tpsApi = policyApi();
    try {
      TpsRetry.retry(
          () ->
              tpsApi.createPao(
                  new TpsPaoCreateRequest()
                      .objectId(objectId)
                      .component(component)
                      .objectType(objectType)
                      .attributes(inputs)));
    } catch (ApiException e) {
      throw convertApiException(e);
    }
  }

  @WithSpan
  public void deletePao(UUID objectId) throws InterruptedException {
    TpsApi tpsApi = policyApi();
    try {
      try {
        TpsRetry.retry(() -> tpsApi.deletePao(objectId));
      } catch (ApiException e) {
        throw convertApiException(e);
      }
    } catch (PolicyServiceNotFoundException e) {
      // If the PAO is not found, it has either already been deleted by a prior step OR it never
      // existed to begin with if the profile had no policies set. Neither case is an error to BPM.
    }
  }

  @WithSpan
  public TpsPaoGetResult getPao(UUID objectId) throws InterruptedException {
    TpsApi tpsApi = policyApi();
    try {
      return TpsRetry.retry(() -> tpsApi.getPao(objectId));
    } catch (ApiException e) {
      throw convertApiException(e);
    }
  }

  @WithSpan
  public TpsPaoGetResult getOrCreatePao(
      UUID objectId, TpsComponent component, TpsObjectType objectType) throws InterruptedException {
    try {
      return getPao(objectId);
    } catch (PolicyServiceNotFoundException ignored) {
      try {
        createPao(objectId, null, component, objectType);
      } catch (PolicyServiceDuplicateException e) {
        // Thrown if the PAO has been created since we failed to get it previously. We don't care if
        // this createPao call is the one to actually create the PAO, we just want the PAO to exist.
        // Drop the DuplicatePAO exception and return the newly created PAO
        logger.info("PAO already created for {}.", objectId);
      }
      return getPao(objectId);
    }
  }

  private RuntimeException convertApiException(ApiException ex) {
    if (ex.getCode() == HttpStatus.UNAUTHORIZED.value()) {
      return new PolicyServiceAuthorizationException(
          "Not authorized to access Terra Policy Service", ex.getCause());
    } else if (ex.getCode() == HttpStatus.NOT_FOUND.value()) {
      return new PolicyServiceNotFoundException("Policy service returns not found exception", ex);
    } else if (ex.getCode() == HttpStatus.BAD_REQUEST.value()
        && StringUtils.containsIgnoreCase(ex.getMessage(), "duplicate")) {
      return new PolicyServiceDuplicateException(
          "Policy service throws duplicate object exception", ex);
    } else if (ex.getCode() == HttpStatus.CONFLICT.value()) {
      return new PolicyConflictException("Policy service throws conflict exception", ex);
    } else {
      return new PolicyServiceAPIException(ex);
    }
  }
}
