package net.whydah.service.httpproxy.inn;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.auth.UserResponseUtil;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static net.whydah.service.httpproxy.inn.UserDeliveryAddressResource.API_PATH;


@RestController
@Path(API_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class UserDeliveryAddressResource {
    static final String API_PATH = "/api";
    private static final Logger log = LoggerFactory.getLogger(UserDeliveryAddressResource.class);
    private static final String logonUrl = Configuration.getString("logonservice");

    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;

    @Autowired
    public UserDeliveryAddressResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository = spaApplicationRepository;
    }

    @GET
    @Path("/{secret}/get_shared_delivery_address/{userTokenId}")
    public Response getSharedDeliveryAddress(@PathParam("secret") String secret,
                                             @PathParam("userTokenId") String userTokenId) {
        log.debug("Invoked getSharedDeliveryAddress");

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        if (applicationToken == null) {
            log.warn("Unable to locate application session from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String data = new CommandInnAPIGetSharedDeliveryAddress(
                URI.create(logonUrl),
                applicationToken.getApplicationTokenId(),
                userTokenId
        ).execute();

        log.debug("Received shared delivery address {}", data);

        if (data == null) {
            return createForbiddenResponseWithHeader(credentialStore.findRedirectUrl(applicationToken.getApplicationName()));
        }
        return UserResponseUtil.createResponseWithHeader(data, credentialStore.findRedirectUrl(applicationToken.getApplicationName()));
    }

    private static Response createForbiddenResponseWithHeader(String redirectUrl) {
        return Response.status(Response.Status.FORBIDDEN)
                .header("Access-Control-Allow-Origin", redirectUrl)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }
}
