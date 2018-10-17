package net.whydah.service.auth;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.inn.api.commands.CommandInnAPICheckSharingConsent;
import net.whydah.service.inn.api.commands.CommandInnAPIGiveSharingConsent;
import net.whydah.service.inn.api.commands.CommandInnAPIRemoveSharingConsent;
import net.whydah.sso.application.types.ApplicationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static net.whydah.service.auth.CoreUserResource.API_PATH;

@RestController
@Path(API_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class UserConsentResource extends CoreUserResource {
    private static final Logger log = LoggerFactory.getLogger(UserConsentResource.class);

    private final SPAApplicationRepository spaApplicationRepository;

    @Autowired
    public UserConsentResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
        super(credentialStore);
        this.spaApplicationRepository = spaApplicationRepository;
    }

    @POST
    @Path("/{secret}/give_sharing_consent/{userTokenId}")
    public Response giveSharingConsent(@Context HttpHeaders headers,
                                       @PathParam("secret") String secret,
                                       @PathParam("userTokenId") String userTokenId) {
        log.info("Invoked giveSharingConsent with secret: {} userTokenId: {} and headers: {}",
                secret, userTokenId, headers.getRequestHeaders());

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);

        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String data = new CommandInnAPIGiveSharingConsent(URI.create(logonUrl),
                applicationToken.getApplicationTokenId(), userTokenId).execute();

        return createResponseWithHeader(data, applicationToken.getApplicationName());
    }

    @POST
    @Path("/{secret}/remove_sharing_consent/{userTokenId}")
    public Response removeSharingConsent(@Context HttpHeaders headers,
                                         @PathParam("secret") String secret,
                                         @PathParam("userTokenId") String userTokenId) {
        log.info("Invoked removeSharingConsent with secret: {} userTokenId: {} and headers: {}",
                secret, userTokenId, headers.getRequestHeaders());

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);

        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String data = new CommandInnAPIRemoveSharingConsent(URI.create(logonUrl),
                applicationToken.getApplicationTokenId(), userTokenId).execute();

        return createResponseWithHeader(data, applicationToken.getApplicationName());
    }

    @GET
    @Path("/{secret}/check_sharing_consent/{userTokenId}")
    public Response checkSharingConsent(@Context HttpHeaders headers,
                                        @PathParam("secret") String secret,
                                        @PathParam("userTokenId") String userTokenId) {
        log.info("Invoked checkSharingConsent with secret: {} userTokenId: {} and headers: {}",
                secret, userTokenId, headers.getRequestHeaders());

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);

        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String data = new CommandInnAPICheckSharingConsent(URI.create(logonUrl),
                applicationToken.getApplicationTokenId(), userTokenId).execute();

        return createResponseWithHeader(data, applicationToken.getApplicationName());
    }
}