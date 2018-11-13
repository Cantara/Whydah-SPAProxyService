package net.whydah.service.auth;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.userauth.CommandCreateTicketForUserTokenID;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUserticket;
import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredential;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

import static net.whydah.service.auth.CoreUserResource.API_PATH;

@RestController
@Path(API_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class UserAuthenticationResource extends CoreUserResource {
    private static final Logger log = LoggerFactory.getLogger(UserAuthenticationResource.class);

    private final SPAApplicationRepository spaApplicationRepository;

    @Autowired
    public UserAuthenticationResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
        super(credentialStore);
        this.spaApplicationRepository = spaApplicationRepository;
    }

    @POST
    @Path("/{secret}/authenticate_user/")
    public Response authenticateUser(@Context HttpServletRequest httpServletRequest,
                                     @Context HttpServletResponse httpServletResponse,
                                     @Context HttpHeaders headers,
                                     @PathParam("secret") String secret,
                                     @FormParam("username") String username,
                                     @FormParam("password") String password) {
        log.info("Invoked authenticateUser with secret: {} and headers: {}", secret, headers.getRequestHeaders());

        if (username == null || password == null) {
            log.warn("Unable to find the user credential, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        UserCredential userCredential = new UserCredential(username, password);

        String ticket = UUID.randomUUID().toString();
        UserToken userToken = getUserTokenFromCredential(applicationToken, userCredential, ticket);

        if (userToken == null) {
            applicationToken = createApplicationToken(applicationToken.getApplicationName(), secret);
            userToken = getUserTokenFromCredential(applicationToken, userCredential, ticket);
        }
        if (userToken == null || !userToken.isValid()) {
            log.warn("Unable to resolve valid UserToken from supplied usercredentials, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String jwt = AdvancedJWTokenUtil.buildJWT(RsaJwkHelper.loadARandomJWK(), userToken, ticket, applicationToken.getApplicationID());

        return createResponseWithHeader(jwt, applicationToken.getApplicationName());
    }

    @POST
    @Path("/{secret}/get_token_from_ticket/{ticket}")
    public Response getJWTFromTicketWithPost(@Context HttpHeaders headers,
                                             @PathParam("secret") String secret,
                                             @PathParam("ticket") String ticket) {
        return getJWTFromTicket(headers, secret, ticket);
    }

    @GET
    @Path("/{secret}/get_token_from_ticket/{ticket}")
    public Response getJWTFromTicketWithGet(@Context HttpHeaders headers,
                                            @PathParam("secret") String secret,
                                            @PathParam("ticket") String ticket) {
        return getJWTFromTicket(headers, secret, ticket);
    }


    private Response getJWTFromTicket(HttpHeaders headers, String secret, String ticket) {
        log.info("Invoked getTokenFromTicket with secret: {} ticket: {} and headers: {}", secret, ticket, headers.getRequestHeaders());

        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        log.debug("Get usertoken from ticket {}", ticket);
        UserToken userToken = getUserTokenFromTicket(applicationToken, ticket);
        if (userToken == null || !userToken.isValid()) {
            log.warn("Unable to resolve valid UserToken from ticket, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        String newTicketId = UUID.randomUUID().toString();
        boolean successfullyCreatedTicket = createNewTicket(applicationToken, newTicketId, userToken.getUserTokenId());
        if (!successfullyCreatedTicket) {
            log.warn("Unable to renew a ticket for this UserToken, returning 500");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        String jwt = AdvancedJWTokenUtil.buildJWT(RsaJwkHelper.loadARandomJWK(), userToken, newTicketId, applicationToken.getApplicationID());

        return createResponseWithHeader(jwt, applicationToken.getApplicationName());
    }

    private ApplicationToken createApplicationToken(String applicationName, String secret) {
        Application application = credentialStore.findApplication(applicationName);

        ApplicationCredential appCredential = new ApplicationCredential(application.getId(), application.getName(),
                application.getSecurity().getSecret());
        String myAppTokenXml = new CommandLogonApplication(URI.create(credentialStore.getWas().getSTS()),
                appCredential).execute();

        ApplicationToken applicationToken = ApplicationTokenMapper.fromXml(myAppTokenXml);

        spaApplicationRepository.add(secret, applicationToken);
        return applicationToken;
    }

    private UserToken getUserTokenFromCredential(ApplicationToken applicationToken, UserCredential userCredential, String ticket) {
        CommandLogonUserByUserCredential commandLogonUserByUserCredential = new CommandLogonUserByUserCredential(
                URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(),
                ApplicationTokenMapper.toXML(applicationToken), userCredential, ticket);
        return UserTokenMapper.fromUserTokenXml(commandLogonUserByUserCredential.execute());
    }

    private UserToken getUserTokenFromTicket(ApplicationToken applicationToken, String ticket) {
        CommandGetUsertokenByUserticket commandGetUsertokenByUserticket = new CommandGetUsertokenByUserticket(
                URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(),
                ApplicationTokenMapper.toXML(applicationToken), ticket);
        return UserTokenMapper.fromUserTokenXml(commandGetUsertokenByUserticket.execute());
    }

    private boolean createNewTicket(ApplicationToken applicationToken, String ticketId, String userTokenId) {
        CommandCreateTicketForUserTokenID cmd = new CommandCreateTicketForUserTokenID(
                URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(),
                ApplicationTokenMapper.toXML(applicationToken), ticketId, userTokenId);
        return cmd.execute();
    }

}
