package net.whydah.service.proxy;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.health.HealthResource;
import net.whydah.sso.application.helpers.ApplicationXpathHelper;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationACL;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.adminapi.application.CommandGetApplication;
import net.whydah.sso.commands.adminapi.user.CommandGetUser;
import net.whydah.sso.commands.appauth.CommandGetApplicationKey;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.userauth.CommandCreateTicketForUserTokenID;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUserticket;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUsertokenId;
import net.whydah.util.Configuration;
import net.whydah.util.CookieManager;
import net.whydah.util.StringXORer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import sun.security.krb5.internal.APOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.whydah.service.proxy.ProxyResource.PROXY_PATH;

@Path(PROXY_PATH)
@Produces(MediaType.TEXT_HTML)
public class ProxyResource {

	
    public static final String PROXY_PATH = "/load";
    public static final String FALLBACk_URL =  Configuration.getString("fallbackurl");
    private static final Logger log = LoggerFactory.getLogger(ProxyResource.class);
    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;
    private static boolean isRunning = false;
    private static ScheduledExecutorService scheduledThreadPool;
    private StringXORer stringXORer= new StringXORer();

    private Map<String,Application> spaSecretMap = new HashMap<String,Application>();




    @Autowired
    public ProxyResource(CredentialStore credentialStore,SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository=spaApplicationRepository;
    }

    @Produces(MediaType.APPLICATION_JSON)

    //HUY: there is no secret, that means this is exposed to everyone
    //However, the key advantage is that we conveniently hide the application secret from exposure 
    @GET
    @Path("/{appname}")
    public Response getProxyRedirect(@Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse, @PathParam("appname") String appname) {
        log.trace("getProxyRedirect");
      
        Application application=findApplication(appname);
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
        log.info("Created secret: part1:{}, part2:{} = secret:{}",secretPart1,application.getId(),secret2);
        spaApplicationRepository.add(secret2,createSessionForApplication(application));

        // 5. store part one of secret in user cookie for the domain of the redircet URI and add it to the Response
        StringBuilder sb = new StringBuilder(findRedirectUrl(application));
        sb.append("=");
        sb.append(secretPart2);
        sb.append(";expires=");
        sb.append(846000);
        sb.append(";path=");
        sb.append("/");
        sb.append(";HttpOnly");
        sb.append(";secure");
//        response.setHeader("SET-COOKIE", sb.toString());

       // 6. create 302-response with part2 of secret in http Location header
        Response mresponse=Response.status(Response.Status.FOUND).header("Location", findRedirectUrl(application)+"?code="+ secretPart1 +"&ticket="+ ticket).header("SET-COOKIE",sb.toString()).build();
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
    private String findRedirectUrl(Application application) {
        String redirectUrl = null;

        if (application != null && application.getAcl() != null) {
            List<ApplicationACL> acls = application.getAcl();
            for (ApplicationACL acl : acls) {
                if (acl.getAccessRights() != null && acl.getAccessRights().contains(ApplicationACL.OAUTH2_REDIRECT)) {
                    redirectUrl = acl.getApplicationACLPath();
                    log.trace("Found redirectpath {} for application {}", redirectUrl, application.getId());
                }
            }
        }

        if (redirectUrl==null){
            redirectUrl=application.getApplicationUrl();
        }
        if (redirectUrl==null){
            redirectUrl= FALLBACk_URL;
        }

        return redirectUrl;
    }


    private Application findApplication(String appName){

        List<Application> applicationList = credentialStore.getWas().getApplicationList();
        log.debug("Found {} applications",applicationList.size());
        Application found = null;
        for (Application application:applicationList){
            log.info("Parsing application: {}",application.getName());

            if (application.getName().equalsIgnoreCase(appName)){
                found = application;
                break;
            }
            if (application.getId().equalsIgnoreCase(appName)){
                found = application;
                break;
            }
        }
        
        if(found!=null){
        	//HUY: we have to use admin function to get the full specification of this app
        	//Problem is the app secret is obfuscated in application list
        	CommandGetApplication app = new CommandGetApplication(URI.create(credentialStore.getWas().getUAS()), credentialStore.getWas().getActiveApplicationTokenId(), credentialStore.getAdminUserTokenId(), found.getId());
        	String result = app.execute();
        	if(result!=null){
        		found = ApplicationMapper.fromJson(result);
        	}
        }
        return found;
    }


}
