package bio.terra.profile.app.controller;

import bio.terra.profile.api.UnauthenticatedApi;
import bio.terra.profile.app.configuration.VersionConfiguration;
import bio.terra.profile.model.SystemStatus;
import bio.terra.profile.model.SystemVersion;
import bio.terra.profile.service.status.ProfileStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class UnauthenticatedApiController implements UnauthenticatedApi {
  private final ProfileStatusService statusService;
  private final SystemVersion currentVersion;

  @Autowired
  public UnauthenticatedApiController(
      ProfileStatusService statusService, VersionConfiguration versionConfiguration) {
    this.statusService = statusService;

    this.currentVersion =
        new SystemVersion()
            .gitTag(versionConfiguration.getGitTag())
            .gitHash(versionConfiguration.getGitHash())
            .github(
                "https://github.com/DataBiosphere/terra-billing-profile-manager/commit/"
                    + versionConfiguration.getGitHash())
            .build(versionConfiguration.getBuild());
  }

  @Override
  public ResponseEntity<SystemStatus> serviceStatus() {
    SystemStatus systemStatus = statusService.getCurrentStatus();
    HttpStatus httpStatus = systemStatus.isOk() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
    return new ResponseEntity<>(systemStatus, httpStatus);
  }

  @Override
  public ResponseEntity<SystemVersion> serviceVersion() {
    return new ResponseEntity<>(currentVersion, HttpStatus.OK);
  }
}
