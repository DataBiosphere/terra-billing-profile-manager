package bio.terra.profile.service.policy;

import bio.terra.common.logging.RequestIdFilter;
import bio.terra.policy.api.TpsApi;
import bio.terra.policy.client.ApiClient;
import bio.terra.policy.client.ApiException;
import bio.terra.policy.model.*;
import bio.terra.profile.app.configuration.PolicyServiceConfiguration;
import bio.terra.profile.service.policy.exception.*;
import io.opencensus.contrib.spring.aop.Traced;
import java.io.IOException;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TpsApiDispatch {

  PolicyServiceConfiguration policyServiceConfiguration;

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
  @Traced
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

  @Traced
  public void deletePao(UUID objectId) throws InterruptedException {
    TpsApi tpsApi = policyApi();
    try {
      try {
        TpsRetry.retry(() -> tpsApi.deletePao(objectId));
      } catch (ApiException e) {
        throw convertApiException(e);
      }
    } catch (PolicyServiceNotFoundException e) {
      // Not found is not an error as far as BPM is concerned.
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
