package net.whydah.service.authapi;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.inn.api.commands.CommandInnAPICheckSharingConsent;
import net.whydah.service.inn.api.commands.CommandInnAPICreateOrUpdateADeliveryAddress;
import net.whydah.service.inn.api.commands.CommandInnAPIDeleteDeliveryAddress;
import net.whydah.service.inn.api.commands.CommandInnAPIGetDeliveryAddresses;
import net.whydah.service.inn.api.commands.CommandInnAPIGetOnlyDeliveryAddresses;
import net.whydah.service.inn.api.commands.CommandInnAPIGiveSharingConsent;
import net.whydah.service.inn.api.commands.CommandInnAPIRemoveSharingConsent;
import net.whydah.service.inn.api.commands.CommandInnAPISelectDeliveryAddress;
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

import org.constretto.annotation.Configure;
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
import java.util.Map;
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
	private static String logonurl = net.whydah.util.Configuration.getString("logonservice");

	@Autowired
	public UserAuthenticationAPIResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
		this.credentialStore = credentialStore;
		this.spaApplicationRepository = spaApplicationRepository;
	}

	//authentication stuff
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

		log.debug("Get usertoken from ticket {}", ticket);
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
	public Response authenticateUser(@Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse, @Context HttpHeaders headers, 
			@PathParam("secret") String secret,
			@FormParam("username") String username,
			@FormParam("password") String password
			) {
		log.info("Invoked authenticate_user with secret: {} and headers: {}", secret, headers.getRequestHeaders());

		if(username==null||password==null){
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
		Application application=credentialStore.findApplication(applicationToken.getApplicationName());

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
		//CookieManager.createAndSetUserTokenCookie(userToken.getUserTokenId(),Integer.parseInt(userToken.getLifespan()) ,httpServletRequest, httpServletResponse);
		String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);

		return Response.ok(getResponseTextJson(userToken, ticket, applicationToken.getApplicationID()))
				.header("Access-Control-Allow-Origin", origin)
				.header("Access-Control-Allow-Credentials", true)
				.header("Access-Control-Allow-Headers", "*")
				.build();

	}

	private String getResponseTextJson(UserToken userToken, String userticket,String applicationId) {
		return AdvancedJWTokenUtil.buildJWT(userToken, userticket,applicationId);
	}

	//consent stuff
	@POST
	@Path("/{secret}/give_sharing_consent/{userTokenId}")
	public Response giveSharingConsent(@PathParam("secret") String secret,  @Context HttpHeaders headers, @PathParam("userTokenId") String userTokenId) {
		log.info("Invoked give_sharing_consent with secret: {} userTokenId: {} and headers: {}", secret, userTokenId, headers.getRequestHeaders());


		ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
		if (applicationToken == null) {
			log.warn("Unable to locate applicationsession from secret, returning 403");
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		Application application=credentialStore.findApplication(applicationToken.getApplicationName());        
		String data = new CommandInnAPIGiveSharingConsent(URI.create(logonurl), applicationToken.getApplicationTokenId(), userTokenId).execute();
		String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);

		Response mresponse = Response.ok(data)
				.header("Access-Control-Allow-Origin", origin)
				.header("Access-Control-Allow-Credentials", true)
				.header("Access-Control-Allow-Headers", "*").build();
		return mresponse;

	}

	@POST
	@Path("/{secret}/remove_sharing_consent/{userTokenId}")
	public Response removeSharingConsent(@PathParam("secret") String secret,  @Context HttpHeaders headers, @PathParam("userTokenId") String userTokenId) {
		log.info("Invoked remove_sharing_consent with secret: {} userTokenId: {} and headers: {}", secret, userTokenId, headers.getRequestHeaders());


		ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
		if (applicationToken == null) {
			log.warn("Unable to locate applicationsession from secret, returning 403");
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		Application application=credentialStore.findApplication(applicationToken.getApplicationName());        
		String data = new CommandInnAPIRemoveSharingConsent(URI.create(logonurl), applicationToken.getApplicationTokenId(), userTokenId).execute();
		String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);

		Response mresponse = Response.ok(data)
				.header("Access-Control-Allow-Origin", origin)
				.header("Access-Control-Allow-Credentials", true)
				.header("Access-Control-Allow-Headers", "*").build();
		return mresponse;

	}

	@GET
	@Path("/{secret}/check_sharing_consent/{userTokenId}")
	public Response checkSharingConsent(@PathParam("secret") String secret,  @Context HttpHeaders headers, @PathParam("userTokenId") String userTokenId) {
		log.info("Invoked check_sharing_consent with secret: {} userTokenId: {} and headers: {}", secret, userTokenId, headers.getRequestHeaders());


		ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
		if (applicationToken == null) {
			log.warn("Unable to locate applicationsession from secret, returning 403");
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		Application application=credentialStore.findApplication(applicationToken.getApplicationName());        
		String data = new CommandInnAPICheckSharingConsent(URI.create(logonurl), applicationToken.getApplicationTokenId(), userTokenId).execute();
		String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);

		Response mresponse = Response.ok(data)
				.header("Access-Control-Allow-Origin", origin)
				.header("Access-Control-Allow-Credentials", true)
				.header("Access-Control-Allow-Headers", "*").build();
		return mresponse;

	}

	//delivery address stuff
	@GET
	@Path("/{secret}/get_delivery_address/{userTokenId}")
	public Response getDeliveryAddress(@PathParam("secret") String secret,  @Context HttpHeaders headers, @PathParam("userTokenId") String userTokenId) {
		log.info("Invoked get_delivery_address with secret: {} userTokenId: {} and headers: {}", secret, userTokenId, headers.getRequestHeaders());


		ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
		if (applicationToken == null) {
			log.warn("Unable to locate applicationsession from secret, returning 403");
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		Application application=credentialStore.findApplication(applicationToken.getApplicationName());        
		String data = new CommandInnAPIGetOnlyDeliveryAddresses(URI.create(logonurl), applicationToken.getApplicationTokenId(), userTokenId).execute();
		String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);

		Response mresponse = Response.ok(data)
				.header("Access-Control-Allow-Origin", origin)
				.header("Access-Control-Allow-Credentials", true)
				.header("Access-Control-Allow-Headers", "*").build();
		return mresponse;

	}

	@GET
	@Path("/{secret}/get_crmdata/{userTokenId}")
	public Response getCrmData(@PathParam("secret") String secret,  @Context HttpHeaders headers, @PathParam("userTokenId") String userTokenId) {
		log.info("Invoked get_crmdata with secret: {} userTokenId: {} and headers: {}", secret, userTokenId, headers.getRequestHeaders());


		ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
		if (applicationToken == null) {
			log.warn("Unable to locate applicationsession from secret, returning 403");
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		Application application=credentialStore.findApplication(applicationToken.getApplicationName());        
		String data = new CommandInnAPIGetDeliveryAddresses(URI.create(logonurl), applicationToken.getApplicationTokenId(), userTokenId).execute();
		log.debug("Received crm data {}", data);
		String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);

		Response mresponse = Response.ok(data)
				.header("Access-Control-Allow-Origin", origin)
				.header("Access-Control-Allow-Credentials", true)
				.header("Access-Control-Allow-Headers", "*").build();
		return mresponse;

	}

	@POST
	@Path("/{secret}/create_delivery_address/{userTokenId}")
	public Response create_deliveryAddress(@PathParam("secret") String secret,  
			@Context HttpHeaders headers, 
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
			@FormParam("select") boolean select
			) {
		log.info("Invoked create_delivery_address with secret: {} userTokenId: {} and headers: {}", secret, userTokenId, headers.getRequestHeaders());


		ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
		if (applicationToken == null) {
			log.warn("Unable to locate applicationsession from secret, returning 403");
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		Application application=credentialStore.findApplication(applicationToken.getApplicationName());        
		String data = new CommandInnAPICreateOrUpdateADeliveryAddress(URI.create(logonurl), 
				applicationToken.getApplicationTokenId(), 
				userTokenId,
				tag, companyName, emailAddress, contactName, phoneNumber,
				countryCode, postalCode, postalCity, mainAddressLine,
				addressLine1, addressLine2, comment, useAsMainAddress, select		
				).execute();


		String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);

		Response mresponse = Response.ok(data)
				.header("Access-Control-Allow-Origin", origin)
				.header("Access-Control-Allow-Credentials", true)
				.header("Access-Control-Allow-Headers", "*").build();
		return mresponse;

	}

	@POST
	@Path("/{secret}/select_delivery_address/{userTokenId}")
	public Response select_deliveryAddress(@PathParam("secret") String secret,  @Context HttpHeaders headers,
			@PathParam("userTokenId") String userTokenId,
			@FormParam("tag") String tag) {
		log.info("Invoked select_delivery_address with secret: {} userTokenId: {} and headers: {}", secret, userTokenId, headers.getRequestHeaders());


		ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
		if (applicationToken == null) {
			log.warn("Unable to locate applicationsession from secret, returning 403");
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		Application application=credentialStore.findApplication(applicationToken.getApplicationName());        
		String data = new CommandInnAPISelectDeliveryAddress(URI.create(logonurl), applicationToken.getApplicationTokenId(), userTokenId, tag).execute();
		
		String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);

		Response mresponse = Response.ok(data)
				.header("Access-Control-Allow-Origin", origin)
				.header("Access-Control-Allow-Credentials", true)
				.header("Access-Control-Allow-Headers", "*").build();
		return mresponse;

	}

	@POST
	@Path("/{secret}/delete_delivery_address/{userTokenId}/{tag}")
	public Response delete_deliveryAddress(@PathParam("secret") String secret,  @Context HttpHeaders headers,
			@PathParam("userTokenId") String userTokenId,
			@FormParam("tag") String tag) {
		log.info("Invoked delete_delivery_address with secret: {} userTokenId: {} and headers: {}", secret, userTokenId, headers.getRequestHeaders());


		ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
		if (applicationToken == null) {
			log.warn("Unable to locate applicationsession from secret, returning 403");
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		Application application=credentialStore.findApplication(applicationToken.getApplicationName());        
		String data = new CommandInnAPIDeleteDeliveryAddress(URI.create(logonurl), applicationToken.getApplicationTokenId(), userTokenId, tag).execute();
		
		String origin = Configuration.getBoolean("allow.origin")?"*":credentialStore.findRedirectUrl(application);

		Response mresponse = Response.ok(data)
				.header("Access-Control-Allow-Origin", origin)
				.header("Access-Control-Allow-Credentials", true)
				.header("Access-Control-Allow-Headers", "*").build();
		return mresponse;

	}
	
	//login/registration stuff
	
	
}
