package bio.terra.profile.app;

import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.common.iam.AuthenticatedUserRequest;
import bio.terra.profile.db.ProfileDao;
import bio.terra.profile.model.CloudPlatform;
import bio.terra.profile.service.iam.model.SamRole;
import bio.terra.profile.service.profile.ProfileService;
import bio.terra.profile.service.profile.model.BillingProfile;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class StaticDataInitializer {

    // todo externalize this
    private static final UUID STATIC_PROFILE_ID = UUID.fromString("0663c1b8-2896-4f59-ad1b-bd51d3c653fe");
    private final Set<String> SAM_OAUTH_SCOPES = ImmutableSet.of("openid", "email", "profile");

    private final ProfileDao profileDao;
    private final ProfileService profileService;
    private static final Logger logger = LoggerFactory.getLogger(StaticDataInitializer.class);

    @Autowired
    public StaticDataInitializer(ProfileService profileService, ProfileDao profileDao) {
        this.profileService = profileService;
        this.profileDao = profileDao;
    }

    public String getBpmServiceAccountToken() {
        try {
            GoogleCredentials creds =
                    GoogleCredentials.getApplicationDefault().createScoped(SAM_OAUTH_SCOPES);
            creds.refreshIfExpired();
            return creds.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new InternalServerErrorException("Internal server error retrieving WSM credentials", e);
        }
    }

    public void initStaticData() {
        var staticBillingProfile = new BillingProfile(
                STATIC_PROFILE_ID,
                "staticBillingProfile",
                "Static Billing Profile",
                "direct",
                CloudPlatform.AZURE,
                Optional.empty(),
                Optional.of(UUID.fromString("fad90753-2022-4456-9b0a-c7e5b934e408")),
                Optional.of(UUID.fromString("df547342-9cfd-44ef-a6dd-df0ede32f1e3")),
                Optional.of("wor717mk2"),
                null,
                null, "fake@example.com");

        if (!profileDao.profileExistsWithId(STATIC_PROFILE_ID)) {
            logger.info("Creating static billing profile [id={}]", STATIC_PROFILE_ID);
            var request = new AuthenticatedUserRequest.Builder().setToken(getBpmServiceAccountToken()).setEmail("aherbst@broadinstitute.org").setSubjectId("empty").build();
            profileService.createProfile(staticBillingProfile, request);
            profileService.addProfilePolicyMember(STATIC_PROFILE_ID, SamRole.OWNER.getSamRoleName(), "harry.potter@quality.firecloud.org", request);
            profileService.addProfilePolicyMember(STATIC_PROFILE_ID, SamRole.OWNER.getSamRoleName(), "aherbst@broadinstitute.org", request);
            logger.info("Created static billing profile [id={}]", STATIC_PROFILE_ID);
        } else {
            logger.info("Static billing profile already exists, skipping creation [id={}]", STATIC_PROFILE_ID);
        }
    }
}
