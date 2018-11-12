package net.whydah.service.proxy;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.userauth.CommandCreateTicketForUserTokenID;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUserticket;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import net.whydah.util.Configuration;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

import static net.whydah.service.CredentialStore.FALLBACK_URL;
import static net.whydah.service.proxy.ProxyResource.PROXY_PATH;

@RestController
@Path(PROXY_PATH)
@Produces(MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ProxyResource {
    public static final String PROXY_PATH = "/load";

    private static final Logger log = LoggerFactory.getLogger(ProxyResource.class);

    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;

    @Autowired
    public ProxyResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository = spaApplicationRepository;
    }

    //HUY: There is trouble with CORS
    //If a site doesn’t send the Access-Control-Allow-Origin header in its responses,
    //then there’s no way the frontend JavaScript code can directly access responses from that site.
    //We can possibly use a CORS proxy https://github.com/Rob--W/cors-anywhere/ but there is some limitation
    //Therefore, I make this for SPA client script
    @GET
    @Path("/api/{appName}")
    public Response initSPASession(@Context HttpServletRequest httpServletRequest,
                                   @Context HttpServletResponse httpServletResponse,
                                   @Context HttpHeaders headers,
                                   @PathParam("appName") String appName) {
        log.info("Invoked initSPASession with appname: {} and headers: {}", appName, headers.getRequestHeaders());
        Application application = credentialStore.findApplication(appName);
        if (application == null) {
            // No registered application found, return to default login
            return redirectToFallbackUrl();
        }

        //try to get a userticket from querystring, this can happen when we possibly retrieve from the localstorage
        String userticket = httpServletRequest.getParameter("ticket");
        String newTicket = renewTicketWithUserTicket(userticket);

        if (newTicket == null) {
            //try finding from cookie possibly? or maybe not?
            String userTokenId = CookieManager.getUserTokenIdFromCookie(httpServletRequest);
            newTicket = renewTicket(userTokenId);
        }

        //TODO: ED, if newTicket is still null, we should probably send an error response

        // 4. establish new SPA secret and store it in secret-applicationsession map
        SPASessionSecret spaSessionSecret = new SPASessionSecret();
        log.info("Created " + spaSessionSecret);
        spaApplicationRepository.add(spaSessionSecret.getSecret(), getOrCreateSessionForApplication(application));

        /*
        if (Configuration.getBoolean("allow.simple.secret")) {
            String simpleSecret = spaSessionSecret.getSimpleSecret(application.getId());
            log.info("Created simple secret: secretPart1={}, applicationId={} = simpleSecret={}", spaSessionSecret.getSecretPart1(), application.getId(), simpleSecret);
            spaApplicationRepository.add(simpleSecret, getOrCreateSessionForApplication(application));
        }
        */

        String origin = Configuration.getBoolean("allow.origin") ? "*" : credentialStore.findRedirectUrl(application);

        //TODO should send secretpart1 in body, not the xor'ed secret.
        //TODO add spaSessionSecret.getSecretPart2() to cookie with secure set and httpOnly not set
        String body = createJSONBody(spaSessionSecret.getSecret(), newTicket).toString();
        return Response.ok(body)
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }

    private String renewTicketWithUserTicket(String userticket) {
        log.debug("User ticket from request param is {}", userticket);

        if (userticket == null) {
            return null;
        }
        String userTokenXml = getUserTokenXml(userticket);
        if (userTokenXml == null) {
            log.debug("User token xml is null");
            return null;
        }

        String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
        return renewTicket(userTokenId);
    }

    private String renewTicket(String userTokenId) {
        log.debug("Renewing userticket using userTokenId={}", userTokenId);

        if (userTokenId == null) {
            return null;
        }

        String newTicket = UUID.randomUUID().toString();
        if (!generateAUserTicket(userTokenId, newTicket)) {
            log.debug("Should not generate a new ticket. Reverting to null");
            newTicket = null;
        } else {
            log.debug("Generated a new ticket");
        }
        return newTicket;
    }




    //ED commented out 2018-11-12 because not used by INN-JS.
    //This is added back for INN-279 (not fully tested just yet). We also have to adjust JS client
    /*
    @GET
    @Path("/{appName}")
    public Response initSPASessionAndRedirectToSPADownload(@Context HttpServletRequest httpServletRequest,
                                                           @Context HttpServletResponse httpServletResponse,
                                                           @Context HttpHeaders headers,
                                                           @PathParam("appName") String appName) {
        log.info("Invoked initSPASessionAndRedirectToSPADownload with appname: {} and headers: {}", appName, headers.getRequestHeaders());

        //ED: 1. look up the SPAApplicationName and match it against configured valid Whydah Applications
        Application application = credentialStore.findApplication(appName);
        if (application == null) {
            // No registered application found, return to default login
            return redirectToFallbackUrl();
        }

        //try to get a userticket from querystring, this can happen when we possibly retrieve from the localstorage
        String userticket = httpServletRequest.getParameter("ticket");
        String newTicket = renewTicketWithUserTicket(userticket);

        if (newTicket == null) {
            log.debug("New ticket is null");
            //try finding from cookie possibly? or maybe not?
            String userTokenId = CookieManager.getUserTokenIdFromCookie(httpServletRequest);
            log.debug("User token id from cookie is {}", userTokenId);
            if (userTokenId != null && generateAUserTicket(userTokenId, newTicket)) {
                log.debug("Should generate a new ticket 2");
                newTicket = UUID.randomUUID().toString();
            } else {
                log.debug("Should not generate a new ticket 2");
            }
        } else {
            log.debug("New ticket is {}", newTicket);
        }


        // 4. establish new SPA secret and store it in secret-applicationsession map
        //ED: 2. it initiates the session, it provision the SPA application with an unique session represented by two secrets
        SPASessionSecret spaSessionSecret = new SPASessionSecret();
        log.info("Created " + spaSessionSecret);

        //ED: 3. it maps the SPA sessions (multiple) onto an single Whydah application session
        spaApplicationRepository.add(spaSessionSecret.getSecret(), getOrCreateSessionForApplication(application));
//        if (Configuration.getBoolean("allow.simple.secret")) {
//            log.info("Created secret: part1:{}, part2:{} = secret:{}", secretPart1, application.getId(), secret2);
//            spaApplicationRepository.add(secret2, getOrCreateSessionForApplication(application));
//        }


        //ED: 4. do a 302-redirect to the URI of the SPA application, loading the SPA application into the user's browser
        String spaRedirectUrl = credentialStore.findRedirectUrl(application);
        return redirectToSPADownload(spaRedirectUrl, spaSessionSecret.getSecretPart1(), newTicket, spaSessionSecret.getSecretPart2());
    }

    private static Response redirectToSPADownload(String spaRedirectUrl, String secretPart1, String newTicket, String secretPart2) {
        String origin = Configuration.getBoolean("allow.origin") ? "*" : spaRedirectUrl;
        String location = spaRedirectUrl + "?code=" + secretPart1 + "&ticket=" + newTicket;
        String setCookie =
                "code=" + secretPart2 +
                        ";expires=" + 846000 +
                        ";path=" + "/" +
                        ";HttpOnly" +
                        ";secure";
        return Response.status(Response.Status.FOUND)
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .header("Access-Control-Expose-Headers", "Cookie")
                .header("Location", location)
                .header("SET-COOKIE", setCookie)
                .build();
    }
    */

    private static Response redirectToFallbackUrl() {
        return Response.status(Response.Status.FOUND)
                .header("Location", FALLBACK_URL)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }


    private String getUserTokenXml(String userticket) {
        return new CommandGetUsertokenByUserticket(URI.create(credentialStore.getWas().getSTS()),
                credentialStore.getWas().getActiveApplicationTokenId(),
                credentialStore.getWas().getActiveApplicationTokenXML(),
                userticket
        ).execute();
    }

    private JSONObject createJSONBody(String secret, String ticket) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("secret", secret);
            if (ticket != null) {
                jsonObject.put("ticket", ticket);
            }
        } catch (JSONException e) {
            log.error("JSON object with secret could not be created", e);
        } finally {
            return jsonObject;
        }
    }

    private boolean generateAUserTicket(String userTokenId, String ticket) {
        CommandCreateTicketForUserTokenID cmt = new CommandCreateTicketForUserTokenID(URI.create(credentialStore.getWas().getSTS()),
                credentialStore.getWas().getActiveApplicationTokenId(), credentialStore.getWas().getActiveApplicationTokenXML(),
                ticket, userTokenId);

        boolean result = cmt.execute();

        if (result) {
            log.debug("create a ticket {} for usertoken {}", ticket, userTokenId);
        } else {
            log.warn("failed to create a ticket {} for usertoken {}", ticket, userTokenId);
        }
        return result;
    }

    private ApplicationToken getOrCreateSessionForApplication(Application application) {
        ApplicationToken applicationToken = getApplicationTokenFromSessions(application);

        if (applicationToken == null) {
            return createApplicationToken(application);
        } else {
            return applicationToken;
        }
    }

    private ApplicationToken createApplicationToken(Application application) {
        ApplicationCredential appCredential = new ApplicationCredential(application.getId(), application.getName(),
                application.getSecurity().getSecret());
        String appTokenXml = new CommandLogonApplication(URI.create(credentialStore.getWas().getSTS()), appCredential).execute();

        return ApplicationTokenMapper.fromXml(appTokenXml);
    }

    private ApplicationToken getApplicationTokenFromSessions(Application application) {
        for (ApplicationToken applicationToken : spaApplicationRepository.allSessions()) {
            if (applicationToken.getApplicationID().equalsIgnoreCase(application.getId())) {
                return applicationToken;
            }
        }
        return null;
    }
}
