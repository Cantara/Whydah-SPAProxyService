package net.whydah.service.proxy;

import net.whydah.service.health.HealthResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.UUID;

import static net.whydah.service.proxy.ProxyResource.PROXY_PATH;

@Path(PROXY_PATH)
@Produces(MediaType.TEXT_HTML)
public class ProxyResource {

    public static final String PROXY_PATH = "/";
    private static final Logger log = LoggerFactory.getLogger(ProxyResource.class);


    @GET
    public Response getProxyRedirect() {
        log.trace("getProxyRedirect");

        // 1. get redirectname from URI
        // 2. lookup redirectname in applicationmodel to find URI to redirect to
        // 3. lookup potential usertokenId from request cookies
        // 4. establish new SPA secret and store it in secret-applicationsession map
        // 5. store part one of secret in user cookie for the domain of the redircet URI and add it to the Response
        // 6. create 302-response with part2 of secret in http Location header

        Response response=Response.status(Response.Status.FOUND).header("Location", "https://www.vg.no?code="+ UUID.randomUUID().toString()+"&ticket="+ UUID.randomUUID().toString()).build();
        return response;

    }

}
