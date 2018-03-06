package net.whydah.service.authapi;

import net.whydah.service.proxy.ProxyResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static net.whydah.service.authapi.UserAuthenticationAPIResource.API_PATH;

@Path(API_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class UserAuthenticationAPIResource {

    public static final String API_PATH = "/api/";
    private static final Logger log = LoggerFactory.getLogger(ProxyResource.class);


    @GET
    public Response getProxyRedirect() {
        log.trace("getProxyRedirect");

        // 1. lookup secret in secret-application session map
        // 2. execute Whydah API request using the found application

        return Response.ok(getResponseTextJson()).build();

    }

    private String getResponseTextJson(){
        return "{}";
    }
}
