package net.whydah.service.inn.proxy;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.inn.proxy.commands.*;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static net.whydah.service.inn.proxy.UserDeliveryAddressResource.API_PATH;


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
    @Path("/{secret}/get_delivery_address/{userTokenId}")
    public Response getDeliveryAddress(@Context HttpHeaders headers,
                                       @PathParam("secret") String secret,
                                       @PathParam("userTokenId") String userTokenId) {
        log.info("Invoked get_delivery_address with secret: {} userTokenId: {} and headers: {}",
                secret, userTokenId, headers.getRequestHeaders());

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);

        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String data = new CommandInnAPIGetOnlyDeliveryAddresses(URI.create(logonUrl),
                applicationToken.getApplicationTokenId(), userTokenId).execute();

        return UserResponseUtil.createResponseWithHeader(data, credentialStore.findRedirectUrl(applicationToken.getApplicationName()));
    }

    @GET
    @Path("/{secret}/get_crmdata/{userTokenId}")
    public Response getCrmData(@Context HttpHeaders headers,
                               @PathParam("secret") String secret,
                               @PathParam("userTokenId") String userTokenId) {
        log.info("Invoked getCrmdata with secret: {} userTokenId: {} and headers: {}",
                secret, userTokenId, headers.getRequestHeaders());

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);

        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String data = new CommandInnAPIGetDeliveryAddresses(URI.create(logonUrl),
                applicationToken.getApplicationTokenId(), userTokenId).execute();
        log.debug("Received crm data {}", data);

        // If the hystrix command returns null, it failed.
        // The most likely cause is that a consent is not in place.
        if (data == null) {
            return UserResponseUtil.createForbiddenResponseWithHeader(credentialStore.findRedirectUrl(applicationToken.getApplicationName()));
        }

        return UserResponseUtil.createResponseWithHeader(data, credentialStore.findRedirectUrl(applicationToken.getApplicationName()));
    }

    @GET
    @Path("/{secret}/get_shared_delivery_address/{userTokenId}")
    public Response getSharedDeliveryAddress(@Context HttpHeaders headers,
                                             @PathParam("secret") String secret,
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
            return UserResponseUtil.createForbiddenResponseWithHeader(credentialStore.findRedirectUrl(applicationToken.getApplicationName()));
        }
        return UserResponseUtil.createResponseWithHeader(data, credentialStore.findRedirectUrl(applicationToken.getApplicationName()));
    }

    @POST
    @Path("/{secret}/create_delivery_address/{userTokenId}")
    public Response createDeliveryAddress(@Context HttpHeaders headers,
                                          @PathParam("secret") String secret,
                                          @PathParam("userTokenId") String userTokenId,
                                          @FormParam("tag") String tag,
                                          @FormParam("default") boolean useAsMainAddress,
                                          @FormParam("company") String companyName,
                                          @FormParam("email") String emailAddress,
                                          @FormParam("name") String contactName,
                                          @FormParam("cellPhone") String phoneNumber,
                                          @FormParam("countryCode") String countryCode,
                                          @FormParam("postalCode") String postalCode,
                                          @FormParam("postalCity") String postalCity,
                                          @FormParam("addressLine") String mainAddressLine,
                                          @FormParam("addressLine1") String addressLine1,
                                          @FormParam("addressLine2") String addressLine2,
                                          @FormParam("comment") String comment,
                                          @FormParam("select") boolean select) {
        log.info("Invoked createDeliveryAddress with secret: {} userTokenId: {} and headers: {}",
                secret, userTokenId, headers.getRequestHeaders());

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);

        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String data = new CommandInnAPICreateOrUpdateADeliveryAddress(URI.create(logonUrl),
                applicationToken.getApplicationTokenId(),
                userTokenId,
                tag, companyName, emailAddress, contactName, phoneNumber,
                countryCode, postalCode, postalCity, mainAddressLine,
                addressLine1, addressLine2, comment, useAsMainAddress, select
        ).execute();

        return UserResponseUtil.createResponseWithHeader(data, credentialStore.findRedirectUrl(applicationToken.getApplicationName()));
    }

    @POST
    @Path("/{secret}/select_delivery_address/{userTokenId}")
    public Response selectDeliveryAddress(@Context HttpHeaders headers,
                                           @PathParam("secret") String secret,
                                           @PathParam("userTokenId") String userTokenId,
                                           @FormParam("tag") String tag) {
        log.info("Invoked selectDeliveryAddress with secret: {} userTokenId: {} and headers: {}",
                secret, userTokenId, headers.getRequestHeaders());

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);

        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String data = new CommandInnAPISelectDeliveryAddress(URI.create(logonUrl),
                applicationToken.getApplicationTokenId(), userTokenId, tag).execute();

        return UserResponseUtil.createResponseWithHeader(data, credentialStore.findRedirectUrl(applicationToken.getApplicationName()));
    }

    @POST
    @Path("/{secret}/delete_delivery_address/{userTokenId}/{tag}")
    public Response deleteDeliveryAddress(@Context HttpHeaders headers,
                                          @PathParam("secret") String secret,
                                          @PathParam("userTokenId") String userTokenId,
                                          @FormParam("tag") String tag) {
        log.info("Invoked deleteDeliveryAddress with secret: {} userTokenId: {} and headers: {}",
                secret, userTokenId, headers.getRequestHeaders());

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);

        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String data = new CommandInnAPIDeleteDeliveryAddress(URI.create(logonUrl),
                applicationToken.getApplicationTokenId(), userTokenId, tag).execute();

        return UserResponseUtil.createResponseWithHeader(data, credentialStore.findRedirectUrl(applicationToken.getApplicationName()));
    }
}
