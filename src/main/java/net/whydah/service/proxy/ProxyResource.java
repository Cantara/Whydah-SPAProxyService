package net.whydah.service.proxy;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.userauth.CommandCreateTicketForUserTokenID;
import net.whydah.util.Configuration;
import net.whydah.util.CookieManager;
import net.whydah.util.StringXORer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.whydah.service.CredentialStore.FALLBACk_URL;
import static net.whydah.service.proxy.ProxyResource.PROXY_PATH;

@RestController
@Path(PROXY_PATH)
@Produces(MediaType.TEXT_HTML)
public class ProxyResource {

	
    public static final String PROXY_PATH = "/load";
    private static final Logger log = LoggerFactory.getLogger(ProxyResource.class);
    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;
    private StringXORer stringXORer= new StringXORer();

    private Map<String,Application> spaSecretMap = new HashMap<String,Application>();




    @Autowired
    public ProxyResource(CredentialStore credentialStore,SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository=spaApplicationRepository;
    }


//    @CrossOrigin(value = "https://latitude.sixtysix.no", allowCredentials = "true",  allowedHeaders = "*")
    @GET
    @Path("/{appname}/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response proxyAppPing(@Context HttpServletRequest request, @Context HttpHeaders headers, @PathParam("appname") String appname, @QueryParam("s1") String secret1) {
        log.info("Invoked proxyAppPing with appname: {} and headers: {}", appname, headers.getRequestHeaders());

        String body="{\"result\": \"pong\"}";
        Application application=credentialStore.findApplication(appname);
        if (application==null) {
            try {
                Cookie codeCookie = CookieManager.getCodeCookie(request);
                if (codeCookie != null) {
                    body = "{\"result\": \"pong\",\n\"secret2\"=\"" + codeCookie.getValue() + "\"}";
                    Response mresponse = Response.status(Response.Status.OK).header("Access-Control-Allow-Origin", credentialStore.findRedirectUrl(application)).header("Access-Control-Allow-Credentials", true).entity(body).build();
                    return mresponse;
                } else {
                    if (secret1 != null) {
                        body = "{\"result\": \"pong\",\n\"secret2\"=\"" + codeCookie.getValue() + "\"}";
                        StringBuilder sb = new StringBuilder();
                        sb.append("secret2=");
                        sb.append(spaSecretMap.get(secret1));
                        sb.append(";expires=");
                        sb.append(100);
                        sb.append(";path=");
                        sb.append("/");
                        sb.append(";HttpOnly");
                        sb.append(";secure");
                        Response mresponse = Response.status(Response.Status.OK).
                                header("Access-Control-Allow-Origin", credentialStore.findRedirectUrl(application)).
                                header("Access-Control-Allow-Credentials", true)
                                .header("SET-COOKIE", sb.toString())
                                .entity(body).build();
                        return mresponse;
                    }
                }
            } catch (Exception e) {
                log.warn("Ping called but no cookies found: ", e);
            }
        }
        Response mresponse = Response.status(Response.Status.OK).header("Access-Control-Allow-Origin",credentialStore.findRedirectUrl(application)).header("Access-Control-Allow-Credentials",true).entity(body).build();
        return mresponse;
    }
    //HUY: there is no secret, that means this is exposed to everyone
    //However, the key advantage is that we conveniently hide the application secret from exposure 
    @GET
    @Path("/{appname}")
    public Response getProxyRedirect(@Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse, @Context HttpHeaders headers,@PathParam("appname") String appname) {
        log.info("Invoked getProxyRedirect with appname: {} and headers: {}", appname, headers.getRequestHeaders());
        Application application=credentialStore.findApplication(appname);
        if (application==null){
            // No registered application found, return to default login
            return Response.status(Response.Status.FOUND).header("Location", FALLBACk_URL).build();
        }
        // 3. lookup potential usertokenId from request cookies
        //we find a INN/Whydah cookie...   picking up usertokenid, verify that it is valid and creating a userticket based upon the valid usertokenid
        String userTokenId = CookieManager.getUserTokenIdFromCookie(httpServletRequest);
        String ticket = UUID.randomUUID().toString();
        if(userTokenId!=null){
        	CommandCreateTicketForUserTokenID cmt = new CommandCreateTicketForUserTokenID(URI.create(credentialStore.getWas().getSTS()), credentialStore.getWas().getActiveApplicationTokenId(), credentialStore.getWas().getActiveApplicationTokenXML(), ticket, userTokenId);
        	boolean result = cmt.execute();
        	if(result){
        		log.debug("create a ticket {} for usertoken {}", ticket, userTokenId);
        	} else {
        		log.warn("failed to create a ticket {} for usertoken {}", ticket, userTokenId);
        	}
        }
        // 4. establish new SPA secret and store it in secret-applicationsession map
        String secretPart1=UUID.randomUUID().toString();
        String secretPart2=UUID.randomUUID().toString();
        String secret = stringXORer.encode(secretPart1,secretPart2);
        String secret2 = stringXORer.encode(secretPart1,application.getId());

        log.info("Created secret: part1:{}, part2:{} = secret:{}",secretPart1,secretPart2,secret);
        spaApplicationRepository.add(secret,createSessionForApplication(application));
        if (Configuration.getBoolean("allow.simple.secret")){
            log.info("Created secret: part1:{}, part2:{} = secret:{}",secretPart1,application.getId(),secret2);
            spaApplicationRepository.add(secret2,createSessionForApplication(application));
        }

        // 5. store part one of secret in user cookie for the domain of the redircet URI and add it to the Response
        StringBuilder sb = new StringBuilder();
        sb.append("code=");
        sb.append(secretPart2);
        sb.append(";expires=");
        sb.append(846000);
        sb.append(";path=");
        sb.append("/");
        sb.append(";HttpOnly");
        sb.append(";secure");
//        response.setHeader("SET-COOKIE", sb.toString());

       // 6. create 302-response with part2 of secret in http Location header
        Response mresponse = Response.status(Response.Status.FOUND)
                .header("Location", credentialStore.findRedirectUrl(application) + "?code=" + secretPart1 + "&ticket=" + ticket)
                .header("Access-Control-Expose-Headers", "Cookie")
                .header("SET-COOKIE", sb.toString()).build();
        return mresponse;

    }

    private ApplicationToken createSessionForApplication(Application application){
        for (ApplicationToken applicationToken:spaApplicationRepository.allSessions()){
            if (applicationToken.getApplicationID().equalsIgnoreCase(application.getId())){
                return applicationToken;
            }
        }
        ApplicationCredential appCredential = new ApplicationCredential(application.getId(),application.getName(),application.getSecurity().getSecret());
        String myAppTokenXml = new CommandLogonApplication(URI.create(credentialStore.getWas().getSTS()), appCredential).execute();
        ApplicationToken applicationToken = ApplicationTokenMapper.fromXml(myAppTokenXml);
        return applicationToken;
    }





}
