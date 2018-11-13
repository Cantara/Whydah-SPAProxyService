package net.whydah.service.proxy;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.sso.application.types.Application;
import net.whydah.util.Configuration;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.Date;

import static net.whydah.service.CredentialStore.FALLBACK_URL;
import static net.whydah.service.proxy.ProxyResource.PROXY_PATH;

@RestController
@Path(PROXY_PATH)
@Produces(MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ProxyResource {
    public static final String PROXY_PATH = "/load";

    private static final Logger log = LoggerFactory.getLogger(ProxyResource.class);

    private final CredentialStore credentialStore;
    private final SPASessionHelper initializer;

    @Autowired
    public ProxyResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.initializer = new SPASessionHelper(credentialStore, spaApplicationRepository);
    }

    /**
     * This endpoint will provision the SPA with two secrets using a redirect to the registered spaRedirectUrl.
     * This is more secure than the /api/{appName} endpoint, so this endpoint should be preferred.
     */
    @GET
    @Path("/{appName}")
    public Response initSPASessionAndRedirectToSPA(@Context HttpServletRequest httpServletRequest,
                                                   @Context HttpHeaders headers,
                                                   @PathParam("appName") String appName) {
        log.info("Invoked initSPASessionAndRedirectToSPA with appname: {} and headers: {}", appName, headers.getRequestHeaders());

        Application application = credentialStore.findApplication(appName);
        if (application == null) {
            return redirectToFallbackUrl();
        }

        String newTicket = renewTicket(httpServletRequest);
        //TODO: ED, if newTicket is still null, we should probably send an error response

        SPASessionSecret spaSessionSecret = initializer.addReferenceToApplicationSession(application);

        return spaRedirectUrl(application, spaSessionSecret, newTicket);
    }

    //HUY: There is trouble with CORS
    //If a site doesn’t send the Access-Control-Allow-Origin header in its responses,
    //then there’s no way the frontend JavaScript code can directly access responses from that site.
    //We can possibly use a CORS proxy https://github.com/Rob--W/cors-anywhere/ but there is some limitation
    //Therefore, I make this for SPA client script
    @GET
    @Path("/api/{appName}")
    public Response initSPASession(@Context HttpServletRequest httpServletRequest,
                                   @Context HttpHeaders headers,
                                   @PathParam("appName") String appName) {
        log.info("Invoked initSPASession with appname: {} and headers: {}", appName, headers.getRequestHeaders());

        Application application = credentialStore.findApplication(appName);
        if (application == null) {
            return redirectToFallbackUrl();
        }

        String newTicket = renewTicket(httpServletRequest);
        //TODO: ED, if newTicket is still null, we should probably send an error response

        SPASessionSecret spaSessionSecret = initializer.addReferenceToApplicationSession(application);

        return okResponse(application, spaSessionSecret, newTicket);
    }

    private String renewTicket(HttpServletRequest httpServletRequest) {
        String userticket = httpServletRequest.getParameter("ticket");
        String newTicket = initializer.renewTicketWithUserTicket(userticket);

        if (newTicket == null) {
            String userTokenId = CookieManager.getUserTokenIdFromCookie(httpServletRequest);
            newTicket = initializer.renewTicket(userTokenId);
        }
        return newTicket;
    }


    private Response spaRedirectUrl(Application application, SPASessionSecret spaSessionSecret, String newTicket) {
        String spaRedirectUrl = credentialStore.findRedirectUrl(application);
        String origin = Configuration.getBoolean("allow.origin") ? "*" : spaRedirectUrl;
        String location = spaRedirectUrl + "?code=" + spaSessionSecret.getSecretPart1() + "&ticket=" + newTicket;
        String setCookie =
                "code=" + spaSessionSecret.getSecretPart2() +
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


    private Response okResponse(Application application, SPASessionSecret spaSessionSecret, String newTicket) {
        String spaRedirectUrl = credentialStore.findRedirectUrl(application);
        String body = createJSONBody(spaSessionSecret.getSecret(), newTicket).toString();

        return Response.ok(body)
                .header("Access-Control-Allow-Origin", spaRedirectUrl)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
//                .cookie(getCookie(spaSessionSecret.getSecretPart2()))
                .build();
    }

//    private static NewCookie getCookie(String secretPart2) {
//        return new NewCookie(
//                "code",
//                secretPart2,
//                "/",
//                "https://inn-webshop-demo-2.capra.tv",
//                1,
//                "",
//                1800, // 30 minutes lifetime
//                new Date(Calendar.getInstance().getTimeInMillis() + (30 * 60000)), // 30 minutes lifetime
//                true,
//                false
//        );
//    }

    private static JSONObject createJSONBody(String secret, String ticket) {
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

    private static Response redirectToFallbackUrl() {
        return Response.status(Response.Status.FOUND)
                .header("Location", FALLBACK_URL)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }
}
