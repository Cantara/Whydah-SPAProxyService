package net.whydah.service.spasession;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.sso.application.types.Application;
import net.whydah.util.Configuration;
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
import javax.ws.rs.core.Response;
import java.util.Map;

import static net.whydah.service.CredentialStore.FALLBACK_URL;
import static net.whydah.service.spasession.SPASessionResource.PROXY_PATH;

@RestController
@Path(PROXY_PATH)
@Produces(MediaType.APPLICATION_JSON_UTF8_VALUE)
public class SPASessionResource {
    public static final String PROXY_PATH = "/load";

    private static final Logger log = LoggerFactory.getLogger(SPASessionResource.class);

    private final CredentialStore credentialStore;
    private final SPASessionHelper initializer;

    @Autowired
    public SPASessionResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.initializer = new SPASessionHelper(credentialStore, spaApplicationRepository);
    }

    /**
     * 1. In SPA open pop-up window with this url.
     * 2. Redirect to SSOLoginWebapp to login the user
     * 3. Redirect back to /load/{appName}
     * 4. Redirect to spa redirect url store in application
     */
    @GET
    @Path("/ssologin/{appName}")
    public Response redirectToSSOLoginWebapp(@Context HttpServletRequest httpServletRequest,
                                             @Context HttpHeaders headers,
                                             @PathParam("appName") String appName) {
        log.info("Invoked redirectToSSOLoginWebapp with appname: {} and headers: {}", appName, headers.getRequestHeaders());

        Application application = credentialStore.findApplication(appName);
        String ssoLoginUrl = Configuration.getString("logonservice");
        String spaProxyUrl = Configuration.getString("myuri");
        Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();
        if (application == null || ssoLoginUrl == null || spaProxyUrl == null) {
            log.warn("Redirecting to fallback URL for request with appName: {} due to null values", appName);
            return redirectToFallbackUrl();
        }
        //redirect to ssoLoginWebapp to login in the user
        return ResponseUtil.ssoLoginRedirectUrl(ssoLoginUrl, spaProxyUrl, application, parameterMap);
    }


    /**
     * This endpoint will provision the SPA with two secrets using a redirect to the registered spaRedirectUrl.
     * This is more secure than the /spasession/{appName} endpoint, so this endpoint should be preferred.
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

        return ResponseUtil.spaRedirectUrl(credentialStore, application, spaSessionSecret, newTicket);
    }

    //HUY: There is trouble with CORS
    //If a site doesn’t send the Access-Control-Allow-Origin header in its responses,
    //then there’s no way the frontend JavaScript code can directly access responses from that site.
    //We can possibly use a CORS spasession https://github.com/Rob--W/cors-anywhere/ but there is some limitation
    //Therefore, I make this for SPA client script
    @GET
    @Path("/spasession/{appName}")
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

        return ResponseUtil.okResponse(credentialStore, application, spaSessionSecret, newTicket);
    }

    private String renewTicket(HttpServletRequest httpServletRequest) {
        // TODO: Temporarily support userticket and ticket. userticket is used by SSOLWA
        // While ticket is used by other clients at the moment.
        String userticket = httpServletRequest.getParameter("userticket");
        if (userticket == null || userticket.isEmpty()) {
            userticket = httpServletRequest.getParameter("ticket");
        }
        String newTicket = initializer.renewTicketWithUserTicket(userticket);


        //ED: 2018-11-13 We do not know of any use case where this will be called
        /*
        if (newTicket == null) {
            String userTokenId = CookieManager.getUserTokenIdFromCookie(httpServletRequest);
            newTicket = initializer.renewTicket(userTokenId);
        }
        */
        return newTicket;
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



    private static Response redirectToFallbackUrl() {
        return Response.status(Response.Status.FOUND)
                .header("Location", FALLBACK_URL)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }
}
