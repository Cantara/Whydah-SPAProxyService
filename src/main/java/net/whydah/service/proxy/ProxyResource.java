package net.whydah.service.proxy;

import net.whydah.service.CredentialStore;
import net.whydah.service.health.HealthResource;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.util.StringXORer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import sun.security.krb5.internal.APOptions;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.net.CookieManager;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.whydah.service.proxy.ProxyResource.PROXY_PATH;

@Path(PROXY_PATH)
@Produces(MediaType.TEXT_HTML)
public class ProxyResource {

    public static final String PROXY_PATH = "/";
    private static final Logger log = LoggerFactory.getLogger(ProxyResource.class);
    private final CredentialStore credentialStore;
    private static boolean isRunning = false;
    private static ScheduledExecutorService scheduledThreadPool;
    private StringXORer stringXORer= new StringXORer();
    private List<Application> applicationsList = new LinkedList<>();

    private Map<String,Application> spaSecretMap = new HashMap<String,Application>();




    @Autowired
    public ProxyResource(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }



    @GET
    public Response getProxyRedirect() {
        log.trace("getProxyRedirect");

        // 1. get redirectname from URI

        // 2. lookup redirectname in applicationmodel to find URI to redirect to
        String appname="ACS";
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
        spaSecretMap.put(secret,application);


        // 5. store part one of secret in user cookie for the domain of the redircet URI and add it to the Response


        StringBuilder sb = new StringBuilder("domain from redirectURI");
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

        Response mresponse=Response.status(Response.Status.FOUND).header("Location", "https://www.vg.no?code="+ secretPart1 +"&ticket="+ UUID.randomUUID().toString()).build();
        return mresponse;

    }

    private Application findApplication(String appName){

        List<Application> applicationList = credentialStore.getWas().getApplicationList();
        for (Application application:applicationList){

            if (application.getName().equalsIgnoreCase(appName)){
                return application;
            }
        }
        return null;
    }


}
