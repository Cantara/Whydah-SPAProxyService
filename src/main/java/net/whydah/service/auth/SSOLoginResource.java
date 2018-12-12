package net.whydah.service.auth;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
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
import java.util.UUID;

import static net.whydah.service.auth.SSOLoginResource.API_PATH;


/**
 *
 */
@RestController
@Path(API_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class SSOLoginResource {
    static final String API_PATH = "/{secretOrAppName}/user/auth/ssologin";
    private static final Logger log = LoggerFactory.getLogger(UserAuthenticationResource.class);
    private static final String INITILIZED_VALUE = "INITIALIZED_VALUE";

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
    public Response initializeUserLogin(@Context HttpServletRequest httpServletRequest,
                                        @Context HttpServletResponse httpServletResponse,
                                        @Context HttpHeaders headers,
                                        @PathParam("secretOrAppName") String secretOrAppName) {
        Application application = getApplicationFromSecretOrAppName(secretOrAppName);
        if (application == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        UUID ssoLoginUUID = UUID.randomUUID();

        URI ssoLoginUrl = buildPopupEntryPointURI(spaProxyBaseUri, secretOrAppName, ssoLoginUUID);
        SSOLoginSession ssoLoginSession = new SSOLoginSession(ssoLoginUUID, INITILIZED_VALUE, application.getName());
        ssoLoginSessionMap.put(ssoLoginUUID, ssoLoginSession);

        String body = "{ \"ssoLoginUrl\" : \"" + ssoLoginUrl + "\", \"ssoLoginUUID\": \"" + ssoLoginUUID + "\"}";
        return Response.ok(body).build();
    }

    /**
     * Generates a Login url for the login session
     * Redirects the user to the login url.
     */
    @GET
    @Path("/{ssoLoginUUID}")
    public Response redirectInitializedUserLogin(@Context HttpServletRequest httpServletRequest,
                                                 @Context HttpServletResponse httpServletResponse,
                                                 @Context HttpHeaders headers,
                                                 @PathParam("secretOrAppName") String secretOrAppName,
                                                 @PathParam("ssoLoginUUID") String ssoLoginUUID,
                                                 @QueryParam("UserCheckout") boolean usercheckout) {
        Application application = getApplicationFromSecretOrAppName(secretOrAppName);

        if (application == null) {
            log.info("redirectInitializedUserLogin called with unknown application name or secret. secretOrAppName: {}", secretOrAppName);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        SSOLoginSession ssoLoginSession = ssoLoginSessionMap.get(UUID.fromString(ssoLoginUUID));
        if (ssoLoginSession == null) {
            log.info("redirectInitializedUserLogin called with unknown ssoLoginUUID. ssoLoginUUID: {}", ssoLoginUUID);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!application.getName().equals(ssoLoginSession.getApplicationName())) {
            log.info("redirectInitializedUserLogin called with application that does not match ssoLoginSession. " +
                    "Returning forbidden. ssoLoginUUID: {}, appName: {}", ssoLoginUUID, application.getName());
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        if (!INITILIZED_VALUE.equals(ssoLoginSession.getStatus())) {
            log.info("redirectInitializedUserLogin called with ssoLoginSession with incorrect status. " +
                    "Returning forbidden. ssoLoginUUID: {}, ssoLoginSession.status: {}", ssoLoginUUID, ssoLoginSession.getStatus());
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        Map<String, String[]> originalParameterMap = httpServletRequest.getParameterMap();

        Map<String, String[]> forwardedParameterMap = new HashMap<>();
        for (Map.Entry<String, String[]> entry : originalParameterMap.entrySet()) {
            forwardedParameterMap.put(entry.getKey(), entry.getValue());
        }
        // Pass through query parameters
        // Overwrite the appName parameter explicitly
        forwardedParameterMap.put("appName", new String[]{application.getName()});


        return ResponseUtil.ssoLoginRedirectUrl(ssoLoginBaseUri, spaProxyBaseUri, application, forwardedParameterMap);
    }


    private Application getApplicationFromSecretOrAppName(final @PathParam("secretOrAppName") String secretOrAppName) {

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secretOrAppName);

        if (applicationToken != null && applicationToken.getApplicationName() != null) {
            return credentialStore.findApplication(applicationToken.getApplicationName());
        }
        return credentialStore.findApplication(secretOrAppName);
    }

    private static URI buildPopupEntryPointURI(String spaProxyBaseURI, String secretOrAppName, UUID ssoLoginUUID) {
        return UriBuilder.fromUri(spaProxyBaseURI)
                .path(secretOrAppName)
                .path("/user/auth/ssologin")
                .path(ssoLoginUUID.toString())
                .build();
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
