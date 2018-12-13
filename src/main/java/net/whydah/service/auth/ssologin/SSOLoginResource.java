package net.whydah.service.auth.ssologin;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.auth.UserAuthenticationResource;
import net.whydah.service.spasession.ResponseUtil;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 *
 */
@RestController
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class SSOLoginResource {
    private static final String WITH_SESSION_PATH = "/application/session/{spaSessionSecret}/user/auth/ssologin";
    private static final String WITHOUT_SESSION_PATH = "/application/{appName}/user/auth/ssologin";
    private static final Logger log = LoggerFactory.getLogger(UserAuthenticationResource.class);
    static final String INITILIZED_VALUE = "INITIALIZED_VALUE";

    private Map<UUID, SSOLoginSession> ssoLoginSessionMap;

    private String spaProxyBaseUri = Configuration.getString("myuri");
    private String ssoLoginBaseUri = Configuration.getString("logonservice");

    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;

    @Autowired
    public SSOLoginResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository = spaApplicationRepository;
        initializeHazelcast();
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
        URI ssoLoginUrl = buildPopupEntryPointURIWithApplicationSession(spaProxyBaseUri, spaSessionSecret, ssoLoginUUID);
        initializeSSOLogin(application, ssoLoginUUID);

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
        URI ssoLoginUrl = buildPopupEntryPointURIWithoutApplicationSession(spaProxyBaseUri, appName, ssoLoginUUID);
        initializeSSOLogin(application, ssoLoginUUID);

        return SSOLoginUtil.initializeSSOLoginResponse(ssoLoginUrl, ssoLoginUUID, application.getApplicationUrl());
    }

    private static URI buildPopupEntryPointURIWithApplicationSession(String spaProxyBaseURI, String sessionSecret, UUID ssoLoginUUID) {
        String path = WITH_SESSION_PATH.replace("{spaSessionSecret}", sessionSecret);
        return UriBuilder.fromUri(spaProxyBaseURI)
                .path(path)
                .path(ssoLoginUUID.toString())
                .build();
    }

    private static URI buildPopupEntryPointURIWithoutApplicationSession(String spaProxyBaseURI, String appName, UUID ssoLoginUUID) {
        String path = WITHOUT_SESSION_PATH.replace("{appName}", appName);
        return UriBuilder.fromUri(spaProxyBaseURI)
                .path(path)
                .path(ssoLoginUUID.toString())
                .build();
    }

    private void initializeSSOLogin(final Application application, final UUID ssoLoginUUID) {
        SSOLoginSession ssoLoginSession = new SSOLoginSession(ssoLoginUUID, INITILIZED_VALUE, application.getName());
        ssoLoginSessionMap.put(ssoLoginUUID, ssoLoginSession);
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
        SSOLoginSession ssoLoginSession = ssoLoginSessionMap.get(uuid);
        Optional<Response> optionalResponse = SSOLoginUtil.verifySSOLoginSession(ssoLoginSession, application, uuid);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }

        Map<String, String[]> forwardedParameterMap = SSOLoginUtil.buildQueryParamsForRedirectUrl(ssoLoginSession.getSsoLoginUUID(), application, httpServletRequest.getParameterMap());

        return ResponseUtil.ssoLoginRedirectUrl(ssoLoginBaseUri, spaProxyBaseUri, application, forwardedParameterMap);
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
        SSOLoginSession ssoLoginSession = ssoLoginSessionMap.get(uuid);
        Optional<Response> optionalResponse = SSOLoginUtil.verifySSOLoginSession(ssoLoginSession, application, uuid);
        if (optionalResponse.isPresent()) {
            return optionalResponse.get();
        }

        Map<String, String[]> forwardedParameterMap = SSOLoginUtil.buildQueryParamsForRedirectUrl(ssoLoginSession.getSsoLoginUUID(), application, httpServletRequest.getParameterMap());


        return ResponseUtil.ssoLoginRedirectUrl(ssoLoginBaseUri, spaProxyBaseUri, application, forwardedParameterMap);
    }




    /**
     * Initializes a hazelcast ssoLoginSessionMap.
     * Uses the hazelcast xml file provided through -Dhazelcast.config.
     * If the property is not provided the hazelcast.xml from classpath is selected.
     */
    private void initializeHazelcast() {
        String xmlFileName = System.getProperty("hazelcast.config");
        log.info("Loading hazelcast configuration from :" + xmlFileName);
        Config hazelcastConfig = new Config();


        if (xmlFileName != null && xmlFileName.length() > 10) {
            try {
                hazelcastConfig = new XmlConfigBuilder(xmlFileName).build();
                log.info("Loading hazelcast configuration from :" + xmlFileName);
            } catch (FileNotFoundException notFound) {
                log.error("Error - not able to load hazelcast.xml configuration.  Using embedded configuration as fallback");
            }
        } else {
            log.warn("Unable to load external hazelcast.xml configuration.  Using configuration from classpath as fallback");
            InputStream configFromClassPath = SPAApplicationRepository.class.getClassLoader().getResourceAsStream("hazelcast.xml");
            hazelcastConfig = new XmlConfigBuilder(configFromClassPath).build();
        }

        hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");
        //hazelcastConfig.getGroupConfig().setName("OID_HAZELCAST");
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        String gridPrefix = "SPAPROXY";
        ssoLoginSessionMap = hazelcastInstance.getMap(gridPrefix + "_userloginSessions");
        log.info("Connecting to ssoLoginSessionMap {}", gridPrefix + "_userloginSessions");
    }

}
