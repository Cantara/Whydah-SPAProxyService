package net.whydah.service.authapi;

import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.*;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import io.jsonwebtoken.*;
import java.util.Date;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.proxy.ProxyResource;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static net.whydah.service.authapi.UserAuthenticationAPIResource.API_PATH;

/*
getJWTTokenFromTicket, loginUserWithUserNameAndPassword   loginUserWithUsernameAndPin  renewUserToken
 */

@Path(API_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class UserAuthenticationAPIResource {

    public static final String API_PATH = "/api/";
    private static final Logger log = LoggerFactory.getLogger(ProxyResource.class);
    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;

    @Autowired
    public UserAuthenticationAPIResource(CredentialStore credentialStore,SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository=spaApplicationRepository;
    }



    @POST
    @Path("/{secret}/get_token_from_ticket/{ticket}")
    public Response getJWTFromTicket(@PathParam("secret") String secret,@PathParam("ticket") String ticket) {
        log.trace("getJWTFromTicket - called with secret:{}",secret);

        // 1. lookup secret in secret-application session map
        ApplicationToken applicationToken= spaApplicationRepository.getApplicationTokenBySecret(secret);
        // 2. execute Whydah API request using the found application

        return Response.ok(getResponseTextJson()).build();

    }
    @POST
    @Path("/{secret}/authenticate_user/")
    public Response authenticateUser(@PathParam("secret") String secret) {
        log.trace("authenticateUser - called with secret:{}",secret);

        // 1. lookup secret in secret-application session map
        ApplicationToken applicationToken= spaApplicationRepository.getApplicationTokenBySecret(secret);
        // 2. execute Whydah API request using the found application

        return Response.ok(getResponseTextJson()).build();

    }

    private String getResponseTextJson(){

        return "{"+createJWT("id","issuer","subject", UserTokenMapper.toJson(new UserToken()),666333)+"}";
    }


    private String createJWT(String id, String issuer, String subject, String whydahJsonToken,long ttlMillis) {

        //The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        //We will sign our JWT with our ApiKey secret
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(getSecret());
        //Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder().setId(id)
                                 .setIssuedAt(now)
                                 .setSubject(subject)
                                 .setIssuer(issuer);
//                                 .setPayload(whydahJsonToken);
//                                 .signWith(signatureAlgorithm, signingKey);

        //if it has been specified, let's add the expiration
        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }

        //Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact();
    }

    private String getSecret(){
        return "yiu";
    }
}
