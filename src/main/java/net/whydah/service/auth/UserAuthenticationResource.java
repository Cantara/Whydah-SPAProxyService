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
import net.whydah.util.AdvancedJWTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
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
                                     @FormParam("password") String password
    ) {
        log.info("Invoked authenticateUser with secret: {} and headers: {}", secret, headers.getRequestHeaders());

        if (username == null || password == null) {
            log.warn("Unable to find the user credential, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        // 1. lookup secret in secret-application session map
        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        // 2. execute Whydah API request using the found application
        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Application application = credentialStore.findApplication(applicationToken.getApplicationName());

        UserCredential userCredential = new UserCredential(username, password);

        String ticket = UUID.randomUUID().toString();
        UserToken userToken = UserTokenMapper.fromUserTokenXml(new CommandLogonUserByUserCredential(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), userCredential, ticket).execute());
        if (userToken == null) {
            // Most likely timeout in application sesssion, lets create a new here..
            ApplicationCredential appCredential = new ApplicationCredential(application.getId(), application.getName(), application.getSecurity().getSecret());
            String myAppTokenXml = new CommandLogonApplication(URI.create(credentialStore.getWas().getSTS()), appCredential).execute();
            applicationToken = ApplicationTokenMapper.fromXml(myAppTokenXml);
            spaApplicationRepository.add(secret, applicationToken);
            userToken = UserTokenMapper.fromUserTokenXml(new CommandLogonUserByUserCredential(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), userCredential, ticket).execute());
        }
        if (userToken == null && !userToken.isValid()) {
            log.warn("Unable to resolve valid UserToken from supplied usercredentials, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return createResponseWithHeader(getResponseTextJson(userToken, ticket, applicationToken.getApplicationID()), applicationToken.getApplicationName());
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

        // 1. lookup secret in secret-application session map
        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        // 2. execute Whydah API request using the found application
        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        log.debug("Get usertoken from ticket {}", ticket);
        UserToken userToken = UserTokenMapper.fromUserTokenXml(new CommandGetUsertokenByUserticket(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), ticket).execute());
        if (userToken == null || !userToken.isValid()) {
            log.warn("Unable to resolve valid UserToken from ticket, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        //create a new ticket
        String newTicket = UUID.randomUUID().toString();
        CommandCreateTicketForUserTokenID cmd = new CommandCreateTicketForUserTokenID(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), newTicket, userToken.getUserTokenId());
        if (!cmd.execute()) {
            log.warn("Unable to renew a ticket for this UserToken, returning 500");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return createResponseWithHeader(getResponseTextJson(userToken, newTicket, applicationToken.getApplicationID()), applicationToken.getApplicationName());
    }

    private String getResponseTextJson(UserToken userToken, String userticket, String applicationId) {
        return AdvancedJWTokenUtil.buildJWT(userToken, userticket, applicationId);
    }
}
