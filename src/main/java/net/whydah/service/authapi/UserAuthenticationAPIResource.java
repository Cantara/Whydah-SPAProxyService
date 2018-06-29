package net.whydah.service.authapi;

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
import net.whydah.sso.user.mappers.UserCredentialMapper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.AdvancedJWTokenUtil;
import net.whydah.util.Configuration;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

import static net.whydah.service.authapi.UserAuthenticationAPIResource.API_PATH;

/*
getJWTTokenFromTicket, loginUserWithUserNameAndPassword   loginUserWithUsernameAndPin  renewUserToken
 */

@Path(API_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class UserAuthenticationAPIResource {

    public static final String API_PATH = "/api";
    private static final Logger log = LoggerFactory.getLogger(UserAuthenticationAPIResource.class);
    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;

    @Autowired
    public UserAuthenticationAPIResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository = spaApplicationRepository;
    }

    @POST
    @Path("/{secret}/get_token_from_ticket/{ticket}")
    public Response getJWTFromTicket(@PathParam("secret") String secret,  @Context HttpHeaders headers,@PathParam("ticket") String ticket) {
        log.info("Invoked get_token_from_ticket with secret: {} ticket: {} and headers: {}", secret, ticket, headers.getRequestHeaders());

        // 1. lookup secret in secret-application session map
        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        // 2. execute Whydah API request using the found application
        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Application application=credentialStore.findApplication(applicationToken.getApplicationName());

        
        UserToken userToken = UserTokenMapper.fromUserTokenXml(new CommandGetUsertokenByUserticket(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), ticket).execute());
        if (userToken==null || !userToken.isValid()) {
            log.warn("Unable to resolve valid UserToken from ticket, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        //create a new ticket
        String newTicket = UUID.randomUUID().toString();
        CommandCreateTicketForUserTokenID cmd = new CommandCreateTicketForUserTokenID(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), newTicket, userToken.getUserTokenId());
        if(!cmd.execute()){
        	 log.warn("Unable to renew a ticket for this UserToken, returning 500");
             return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        
        String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);
        
        return Response.ok(getResponseTextJson(userToken, newTicket, applicationToken.getApplicationID()))
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }

    @GET
    @Path("/{secret}/get_token_from_ticket/{ticket}")
    public Response getJWTFromTicket2(@PathParam("secret") String secret,  @Context HttpHeaders headers,@PathParam("ticket") String ticket) {
        log.info("Invoked get_token_from_ticket with secret: {} ticket: {} and headers: {}", secret, ticket, headers.getRequestHeaders());

        // 1. lookup secret in secret-application session map
        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        // 2. execute Whydah API request using the found application
        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Application application=credentialStore.findApplication(applicationToken.getApplicationName());

        
        UserToken userToken = UserTokenMapper.fromUserTokenXml(new CommandGetUsertokenByUserticket(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), ticket).execute());
        if (userToken==null || !userToken.isValid()) {
            log.warn("Unable to resolve valid UserToken from ticket, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        //create a new ticket
        String newTicket = UUID.randomUUID().toString();
        CommandCreateTicketForUserTokenID cmd = new CommandCreateTicketForUserTokenID(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), newTicket, userToken.getUserTokenId());
        if(!cmd.execute()){
        	 log.warn("Unable to renew a ticket for this UserToken, returning 500");
             return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        
        String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);
        
        return Response.ok(getResponseTextJson(userToken, newTicket, applicationToken.getApplicationID()))
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }


    @POST
    @Path("/{secret}/authenticate_user/")
    public Response authenticateUser(@Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse, @Context HttpHeaders headers, @PathParam("secret") String secret, @RequestBody String payload) {
        log.info("Invoked authenticate_user with secret: {} and headers: {}", secret, headers.getRequestHeaders());

        // 1. lookup secret in secret-application session map
        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        // 2. execute Whydah API request using the found application
        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Application application=credentialStore.findApplication(applicationToken.getApplicationName());

        UserCredential userCredential = UserCredentialMapper.fromXml(payload);
        if(userCredential==null){
        	try {
				JSONObject obj = new JSONObject(payload);
				String username = obj.getString("username");
				String password = obj.getString("password");
				userCredential = new UserCredential(username, password);
			} catch (JSONException e) {
				
			}
        }
        if(payload.contains("&")) {
        	String[] pairs = payload.split("&", 2);
        	String username=null, password=null;
        	for (String pair : pairs) {
        		int idx = pair.indexOf("=");
        		String key = pair.substring(0, idx);
        		if(key.equals("username")) {
        			username = pair.substring(idx + 1);
        		} else if(key.equals("password")) {
        			password = pair.substring(idx + 1);
        		}
        	}
        	if(username!=null && password!=null) {
        		userCredential = new UserCredential(username, password);
        	}
        }
        
        if(userCredential==null){
        	 log.warn("Unable to find the user credential, returning 403");
             return Response.status(Response.Status.FORBIDDEN).build();
        }
        
        String ticket = UUID.randomUUID().toString();
        UserToken userToken = UserTokenMapper.fromUserTokenXml(new CommandLogonUserByUserCredential(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), userCredential, ticket).execute());
        if (userToken == null) {
            // Most likely timeout in application sesssion, lets create a new here..
            ApplicationCredential appCredential = new ApplicationCredential(application.getId(), application.getName(), application.getSecurity().getSecret());
            String myAppTokenXml = new CommandLogonApplication(URI.create(credentialStore.getWas().getSTS()), appCredential).execute();
            applicationToken = ApplicationTokenMapper.fromXml(myAppTokenXml);
            spaApplicationRepository.add(secret, applicationToken);
        }
        if (userToken != null && !userToken.isValid()) {
            log.warn("Unable to resolve valid UserToken from supplied usercredentials, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        //CookieManager.createAndSetUserTokenCookie(userToken.getUserTokenId(),Integer.parseInt(userToken.getLifespan()) ,httpServletRequest, httpServletResponse);
        String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);
        
        return Response.ok(getResponseTextJson(userToken, ticket, applicationToken.getApplicationID()))
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();

    }

    private String getResponseTextJson(UserToken userToken, String userticket,String applicationId) {
    	return AdvancedJWTokenUtil.buildJWT(userToken, userticket,applicationId);
    }



}
