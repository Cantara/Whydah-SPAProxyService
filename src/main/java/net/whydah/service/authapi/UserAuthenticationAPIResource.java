package net.whydah.service.authapi;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.proxy.ProxyResource;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.userauth.CommandCreateTicketForUserTokenID;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUserticket;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUsertokenId;
import net.whydah.sso.commands.userauth.CommandLogonUserByUserCredential;
import net.whydah.sso.user.mappers.UserCredentialMapper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.sso.user.types.UserToken;
import net.whydah.util.AdvancedJWTokenUtil;
import net.whydah.util.JWTokenUtil;

import org.jose4j.jwt.JwtClaims;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import static net.whydah.service.authapi.UserAuthenticationAPIResource.API_PATH;

/*
getJWTTokenFromTicket, loginUserWithUserNameAndPassword   loginUserWithUsernameAndPin  renewUserToken
 */

@Path(API_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class UserAuthenticationAPIResource {

    public static final String API_PATH = "/api";
    private static final Logger log = LoggerFactory.getLogger(ProxyResource.class);
    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;

    @Autowired
    public UserAuthenticationAPIResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository = spaApplicationRepository;
    }


    @POST
    @Path("/{secret}/get_token_from_ticket/{ticket}")
    public Response getJWTFromTicket(@PathParam("secret") String secret, @PathParam("ticket") String ticket) {
        log.trace("getJWTFromTicket - called with secret:{}", secret);

        // 1. lookup secret in secret-application session map
        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        // 2. execute Whydah API request using the found application
        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        
        UserToken userToken = UserTokenMapper.fromUserTokenXml(new CommandGetUsertokenByUserticket(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), ticket).execute());
        if (userToken==null || !userToken.isValid()) {
            log.warn("Unable to resolve valid UserToken from ticket, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        //create a new ticket
        String newTicket = UUID.randomUUID().toString();
        CommandCreateTicketForUserTokenID cmd = new CommandCreateTicketForUserTokenID(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), ticket, userToken.getUserTokenId());
        if(!cmd.execute()){
        	 log.warn("Unable to renew a ticket for this UserToken, returning 500");
             return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(getResponseTextJson(userToken, newTicket)).build();
    }


    @POST
    @Path("/{secret}/authenticate_user/")
    public Response authenticateUser(@PathParam("secret") String secret, @RequestBody String payload) {
        log.trace("authenticateUser - called with secret:{}", secret);

        // 1. lookup secret in secret-application session map
        ApplicationToken applicationToken = spaApplicationRepository.getApplicationTokenBySecret(secret);
        // 2. execute Whydah API request using the found application
        if (applicationToken == null) {
            log.warn("Unable to locate applicationsession from secret, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
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
        if(userCredential==null){
        	 log.warn("Unable to find the user credential, returning 403");
             return Response.status(Response.Status.FORBIDDEN).build();
        }
        
        String ticket = UUID.randomUUID().toString();
        UserToken userToken = UserTokenMapper.fromUserTokenXml(new CommandLogonUserByUserCredential(URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(), ApplicationTokenMapper.toXML(applicationToken), userCredential, ticket).execute());

        if (!userToken.isValid()) {
            log.warn("Unable to resolve valid UserToken from supplied usercredentials, returning 403");
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok(getResponseTextJson(userToken, ticket)).build();

    }

    private String getResponseTextJson(UserToken userToken, String userticket) {
    	return AdvancedJWTokenUtil.buildJWT(userToken, userticket);
    }


//    private String createJWT(String id, String issuer, String subject, String whydahJsonToken, long ttlMillis) {
//
//        //The JWT signature algorithm we will be using to sign the token
//        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
//
//        long nowMillis = System.currentTimeMillis();
//        Date now = new Date(nowMillis);
//
//        //We will sign our JWT with our ApiKey secret
//        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(getSecret());
//        //Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
//
//        //Let's set the JWT Claims
//        JwtBuilder builder = Jwts.builder().setId(id)
//                                 .setIssuedAt(now)
//                                 .setSubject(subject)
//                                 .setIssuer(issuer);
////                                 .setPayload(whydahJsonToken);
////                                 .signWith(signatureAlgorithm, signingKey);
//
//        //if it has been specified, let's add the expiration
//        if (ttlMillis >= 0) {
//            long expMillis = nowMillis + ttlMillis;
//            Date exp = new Date(expMillis);
//            builder.setExpiration(exp);
//        }
//
//        //Builds the JWT and serializes it to a compact, URL-safe string
//        return builder.compact();
//    }
//
//    private String getSecret() {
//        return "yiu";
//    }
}
