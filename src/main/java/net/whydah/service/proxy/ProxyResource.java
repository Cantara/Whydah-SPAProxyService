package net.whydah.service.proxy;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.health.HealthResource;
import net.whydah.sso.application.helpers.ApplicationXpathHelper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationACL;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.util.StringXORer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import sun.security.krb5.internal.APOptions;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.CookieManager;
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

    @GET
    @Path("/{appname}")
    public Response getProxyRedirect(@PathParam("appname") String appname) {
        log.trace("getProxyRedirect");

        // 1. get redirectname from URI

        // 2. lookup redirectname in applicationmodel to find URI to redirect to
        //String appname="Whydah-TestWebApplication";
        Application application=findApplication(appname);
        if (application==null){
            // No registered application found, return to default login
            return Response.status(Response.Status.FOUND).header("Location", "https://whydahdev.cantara.on/sso/login").build();
        }
        // 3. lookup potential usertokenId from request cookies
        // 4. establish new SPA secret and store it in secret-applicationsession map
        String secretPart1=UUID.randomUUID().toString();
        String secretPart2=UUID.randomUUID().toString();
        String secret = stringXORer.encode(secretPart1,secretPart2);

        log.info("Created secret: part1:{}, part2:{} = secret:{}",secretPart1,secretPart2,secret);
        spaApplicationRepository.add(secret,createSessionForApplication(application));


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

        Response mresponse=Response.status(Response.Status.FOUND).header("Location", findRedirectUrl(application)+"?code="+ secretPart1 +"&ticket="+ UUID.randomUUID().toString()).header("SET-COOKIE",sb.toString()).build();
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

        return redirectUrl;
    }


    private Application findApplication(String appName){

        List<Application> applicationList = credentialStore.getWas().getApplicationList();
        log.debug("Found {} applications",applicationList.size());
        for (Application application:applicationList){
            log.info("Parsing application: {}",application.getName());

            if (application.getName().equalsIgnoreCase(appName)){
                return application;
            }
            if (application.getId().equalsIgnoreCase(appName)){
                return application;
            }
        }
        return null;
    }


}
