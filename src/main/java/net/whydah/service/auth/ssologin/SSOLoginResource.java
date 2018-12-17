package net.whydah.service.auth.ssologin;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.auth.AdvancedJWTokenUtil;
import net.whydah.service.auth.SPAKeyStoreRepository;
import net.whydah.service.auth.UserResponseUtil;
import net.whydah.service.spasession.SPASessionResource;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.whydah.service.auth.ssologin.SSOLoginUtil.buildQueryParamsForRedirectUrl;
import static net.whydah.service.auth.ssologin.SSOLoginUtil.verifySSOLoginSession;

/**
 *
 */
@RestController
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class SSOLoginResource {
    static final String WITH_SESSION_PATH = "/application/session/{spaSessionSecret}/user/auth/ssologin";
    static final String WITHOUT_SESSION_PATH = "/application/{appName}/user/auth/ssologin";

    private static final Logger log = LoggerFactory.getLogger(SSOLoginResource.class);

    private SSOLoginRepository ssoLoginRepository;

    private String spaProxyBaseUri = Configuration.getString("myuri");
    private String ssoLoginBaseUri = Configuration.getString("logonservice");

    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;
    private final SPAKeyStoreRepository spaKeyStoreRepository;

    @Autowired
    public SSOLoginResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository,
                            SPAKeyStoreRepository spaKeyStoreRepository, SSOLoginRepository ssoLoginRepository) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository = spaApplicationRepository;
        this.spaKeyStoreRepository = spaKeyStoreRepository;
        this.ssoLoginRepository = ssoLoginRepository;
    }

    /**
     * 1. Generates an UUID for the login session. <br>
     * 2. Put the UUID in a shared hazelcast map (ssoLoginSessionMap). <br>
     * 3. Build the URL the client should use to continue to the login attempt. <br>
     * 4. Return the URL and the UUID in Response body. <br>
     */
    @POST
    @Path(WITH_SESSION_PATH)
    public Response initializeSSOLoginWithApplicationSession(@Context HttpServletRequest httpServletRequest,
                                                             @Context HttpServletResponse httpServletResponse,
                                                             @Context HttpHeaders headers,
                                                             @PathParam("spaSessionSecret") String spaSessionSecret) {
        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(spaSessionSecret);

        if (applicationToken == null || applicationToken.getApplicationName() == null) {
            log.info("ApplicationToken not found for spaSessionSecret. Returning UNAUTHORIZED.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Application application = credentialStore.findApplication(applicationToken.getApplicationName());
        if (application == null) {
            log.info("Application not found for applicationName {}. Returning not found.", applicationToken.getApplicationName());
            return Response.status(Response.Status.NOT_FOUND).build();
        }


        UUID ssoLoginUUID = UUID.randomUUID();
        URI ssoLoginUrl = SSOLoginUtil.buildPopupEntryPointURIWithApplicationSession(spaProxyBaseUri, spaSessionSecret, ssoLoginUUID);
        initializeSSOLogin(application, ssoLoginUUID, true);

        return SSOLoginUtil.initializeSSOLoginResponse(ssoLoginUrl, ssoLoginUUID, application.getApplicationUrl());
    }

    /**
     * 1. Generates an UUID for the login session. <br>
     * 2. Put the UUID in a shared hazelcast map (ssoLoginSessionMap). <br>
     * 3. Build the URL the client should use to continue to the login attempt. <br>
     * 4. Return the URL and the UUID in Response body. <br>
     */
    @POST
    @Path(WITHOUT_SESSION_PATH)
    public Response initializeSSOLoginWithoutApplicationSession(@Context HttpServletRequest httpServletRequest,
                                                                @Context HttpServletResponse httpServletResponse,
                                                                @Context HttpHeaders headers,
                                                                @PathParam("appName") String appName) {
        Application application = credentialStore.findApplication(appName);
        if (application == null) {
            log.info("Application not found for appliCationName {}. Returning not found", appName);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        UUID ssoLoginUUID = UUID.randomUUID();
        URI ssoLoginUrl = SSOLoginUtil.buildPopupEntryPointURIWithoutApplicationSession(spaProxyBaseUri, appName, ssoLoginUUID);
        initializeSSOLogin(application, ssoLoginUUID, false);

        return SSOLoginUtil.initializeSSOLoginResponse(ssoLoginUrl, ssoLoginUUID, application.getApplicationUrl());
    }

    private void initializeSSOLogin(final Application application, final UUID ssoLoginUUID, final boolean hasSpaSessionSecret) {
        SSOLoginSession ssoLoginSession = new SSOLoginSession(ssoLoginUUID, SessionStatus.INITIALIZED, application.getName(), hasSpaSessionSecret);
        ssoLoginRepository.put(ssoLoginUUID, ssoLoginSession);
    }



    /**
     * Generates a Login url for the login session
     * Redirects the user to the login url.
     */
    @GET
    @Path(WITH_SESSION_PATH + "/{ssoLoginUUID}")
    public Response redirectInitializedUserLoginWithApplicationSession(@Context HttpServletRequest httpServletRequest,
                                                                       @Context HttpServletResponse httpServletResponse,
                                                                       @Context HttpHeaders headers,
                                                                       @PathParam("spaSessionSecret") String spaSessionSecret,
                                                                       @PathParam("ssoLoginUUID") String ssoLoginUUID) {
        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(spaSessionSecret);

        if (applicationToken == null || applicationToken.getApplicationName() == null) {
            log.debug("redirectInitializedUserLoginWithApplicationSession called with unknown secret. spaSessionSecret: {}", spaSessionSecret);
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Application application = credentialStore.findApplication(applicationToken.getApplicationName());
        if (application == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        UUID uuid = UUID.fromString(ssoLoginUUID);
        SSOLoginSession ssoLoginSession = ssoLoginRepository.get(uuid);
        Optional<Response> optionalResponse = verifySSOLoginSession(ssoLoginSession, application, uuid, SessionStatus.INITIALIZED);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        ssoLoginSession.withStatus(SessionStatus.REDIRECTED);
        ssoLoginRepository.put(uuid, ssoLoginSession);

        Map<String, String[]> forwardedParameterMap = buildQueryParamsForRedirectUrl(ssoLoginSession.getSsoLoginUUID(), application, httpServletRequest.getParameterMap());

        return SSOLoginUtil.ssoLoginRedirectUrl(ssoLoginBaseUri, spaProxyBaseUri, application, forwardedParameterMap, uuid);
    }

    /**
     * Generates a Login url for the login session
     * Redirects the user to the login url.
     */
    @GET
    @Path(WITHOUT_SESSION_PATH + "/{ssoLoginUUID}")
    public Response redirectInitializedUserLoginWithoutApplicationSession(@Context HttpServletRequest httpServletRequest,
                                                                          @Context HttpServletResponse httpServletResponse,
                                                                          @Context HttpHeaders headers,
                                                                          @PathParam("appName") String appName,
                                                                          @PathParam("ssoLoginUUID") String ssoLoginUUID) {
        Application application = credentialStore.findApplication(appName);
        if (application == null) {
            log.info("redirectInitializedUserLogin called with unknown application name. appName: {}", appName);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        UUID uuid = UUID.fromString(ssoLoginUUID);
        SSOLoginSession ssoLoginSession = ssoLoginRepository.get(uuid);
        Optional<Response> optionalResponse = verifySSOLoginSession(ssoLoginSession, application, uuid, SessionStatus.INITIALIZED);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        ssoLoginSession.withStatus(SessionStatus.REDIRECTED);
        ssoLoginRepository.put(uuid, ssoLoginSession);

        Map<String, String[]> forwardedParameterMap = buildQueryParamsForRedirectUrl(ssoLoginSession.getSsoLoginUUID(), application, httpServletRequest.getParameterMap());


        return SSOLoginUtil.ssoLoginRedirectUrl(ssoLoginBaseUri, spaProxyBaseUri, application, forwardedParameterMap, uuid);
    }


    /**
     * Stores the userTicket in the ssoLoginSessionMap.
     * Redirects the user to the matching application location stored in whydah.
     */
    @GET
    @Path(WITHOUT_SESSION_PATH + "/{ssoLoginUUID}/finalize")
    public Response finalizeSSOUserLogin(@Context HttpServletRequest httpServletRequest,
                                         @Context HttpServletResponse httpServletResponse,
                                         @Context HttpHeaders headers,
                                         @PathParam("appName") String appName,
                                         @PathParam("ssoLoginUUID") String ssoLoginUUID,
                                         @QueryParam("userticket") String userticket) {
        Application application = credentialStore.findApplication(appName);
        if (application == null) {
            log.info("finalizeSSOUserLogin called with unknown application name. appName: {}", appName);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        UUID uuid = UUID.fromString(ssoLoginUUID);
        SSOLoginSession ssoLoginSession = ssoLoginRepository.get(uuid);
        Optional<Response> optionalResponse = verifySSOLoginSession(ssoLoginSession, application, uuid, SessionStatus.REDIRECTED);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        ssoLoginSession.withStatus(SessionStatus.COMPLETE);
        ssoLoginSession.withUserTicket(userticket);
        ssoLoginRepository.put(uuid, ssoLoginSession);

        if (ssoLoginSession.hasSpaSessionSecret()) {
            String location = UriBuilder.fromUri(credentialStore.findRedirectUrl(application))
                    .build().toString();
            return Response.status(Response.Status.FOUND)
                    .header("Location", location)
                    .build();
        } else {
            String location = UriBuilder.fromUri(Configuration.getString("myuri"))
                    .path(SPASessionResource.PROXY_PATH)
                    .path(application.getName())
                    .build().toString();
            return Response.status(Response.Status.FOUND)
                    .header("Location", location)
                    .build();
        }
    }

    @DELETE
    @Path(WITH_SESSION_PATH + "/{ssoLoginUUID}")
    public Response getJWTFromSSOLoginSession(@Context HttpServletRequest httpServletRequest,
                                              @Context HttpServletResponse httpServletResponse,
                                              @Context HttpHeaders headers,
                                              @PathParam("spaSessionSecret") String spaSessionSecret,
                                              @PathParam("ssoLoginUUID") String ssoLoginUUID) throws Exception {
        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(spaSessionSecret);

        if (applicationToken == null || applicationToken.getApplicationName() == null) {
            log.debug("redirectInitializedUserLoginWithApplicationSession called with unknown secret. spaSessionSecret: {}", spaSessionSecret);
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Application application = credentialStore.findApplication(applicationToken.getApplicationName());
        if (application == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        UUID uuid = UUID.fromString(ssoLoginUUID);
        SSOLoginSession ssoLoginSession = ssoLoginRepository.get(uuid);
        Optional<Response> optionalResponse = verifySSOLoginSession(ssoLoginSession, application, uuid, SessionStatus.COMPLETE);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }
        String userTicket = ssoLoginSession.getUserTicket();

        UserToken userToken = SSOLoginUtil.getUserToken(credentialStore, applicationToken, userTicket);

        if (userToken == null || !userToken.isValid()) {
            log.warn("Unable to resolve valid UserToken from ticket, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String jwt = AdvancedJWTokenUtil.buildJWT(spaKeyStoreRepository.getARandomKey(), userToken, userTicket, applicationToken.getApplicationID());
        ssoLoginRepository.remove(uuid);

        return UserResponseUtil.createResponseWithHeader(jwt, credentialStore.findRedirectUrl(applicationToken.getApplicationName()));

    }

}
