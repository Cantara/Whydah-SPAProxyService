package net.whydah.util;

import java.security.Key;
import java.util.Arrays;
import java.util.List;












import net.whydah.service.SPAApplicationRepository;
import net.whydah.service.proxy.ProxyResource;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//The jose.4.j library is an open source (Apache 2.0) implementation of JWT and the JOSE specification suite. It is written in Java and relies solely on the JCA APIs for cryptography.
//
//JSON Web Token (JWT) is a compact, URL-safe means of representing claims to be transferred between two parties. JWT is the identity token format in OpenID Connect and it is also widely used in OAuth 2.0 and many other contexts that require compact message security.
//
//The JWT code examples page shows how to easily produce and consume JWTs using this library.
//
//JOSE is short for Javascript Object Signing and Encryption, which is the IETF Working Group that developed the JSON Web Signature (JWS), JSON Web Encryption (JWE) and JSON Web Key (JWK) specifications. JWS and JWE use JSON and base64url encoding to secure messages in a (relatively) simple, compact and web safe format while JWK defines a JSON representation of cryptographic keys. The actual algorithms for JWS, JWE and JWK are defined in JSON Web Algorithms (JWA).
//
//The library supports the JWS/JWE compact serializations with the complete suite of JOSE algorithms. A more detailed breakdown is available below in the Algorithm Support section.
public class AdvancedJWTokenUtil {
	private static final Logger log = LoggerFactory.getLogger(AdvancedJWTokenUtil.class);
	static RsaJsonWebKey rsaJsonWebKey = RsaJwkProducer.produce();
	
	
	public static String buildJWT(UserToken usertoken, String userTicket) {
		
		System.out.println("RSA hash code... " + rsaJsonWebKey.hashCode());
		JwtClaims claims = new JwtClaims();
		claims.setSubject(usertoken.getUserName()); // the subject/principal is whom the token is about
		claims.setJwtId(usertoken.getUserTokenId());
		claims.setIssuer(usertoken.getIssuer());
		claims.setAudience("");// to whom the token is intended to be sent
		claims.setIssuedAtToNow();
		if(userTicket!=null){
			claims.setClaim("userticket", userTicket);
		} else {
			for (UserApplicationRoleEntry userApplicationRoleEntry: usertoken.getRoleList()){
				claims.setClaim(userApplicationRoleEntry.getApplicationName(),userApplicationRoleEntry.getRoleName());

			}
		}
		//add expiration date
		NumericDate numericDate = NumericDate.now();
		numericDate.addSeconds(Long.parseLong(usertoken.getLifespan())/1000);
		claims.setExpirationTime(numericDate);

		JsonWebSignature jws = new JsonWebSignature();
		jws.setPayload(claims.toJson());
	
		jws.setKey(rsaJsonWebKey.getPrivateKey());
		jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

		String jwt = null;
		try {
			jwt = jws.getCompactSerialization();
		} catch (JoseException ex) {
			log.error("failed to generate JWT");
		}

		System.out.println("Claim:\n" + claims);
		System.out.println("JWS:\n" + jws);
		System.out.println("JWT:\n" + jwt);
		return jwt;
	}

	public static JwtClaims parseJWT(String jwt){ 
		RsaJsonWebKey rsaJsonWebKey = RsaJwkProducer.produce();
		// Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
		// be used to validate and process the JWT.
		// The specific validation requirements for a JWT are context dependent, however,
		// it typically advisable to require a (reasonable) expiration time, a trusted issuer, and
		// and audience that identifies your system as the intended recipient.
		// If the JWT is encrypted too, you need only provide a decryption key or
		// decryption key resolver to the builder.
		JwtConsumer jwtConsumer = new JwtConsumerBuilder()
		.setRequireExpirationTime() // the JWT must have an expiration time
		.setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
		.setRequireSubject() // the JWT must have a subject claim
		.setVerificationKey(rsaJsonWebKey.getKey()) // verify the signature with the public key
		.setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s) in the given context
				new AlgorithmConstraints(ConstraintType.WHITELIST, // which is only RS256 here
						AlgorithmIdentifiers.RSA_USING_SHA256))
						.build(); // create the JwtConsumer instance

		try
		{
			//  Validate the JWT and process it to the Claims
			JwtClaims jwtClaims = jwtConsumer.processToClaims(jwt);
			log.info("JWT validation succeeded! " + jwtClaims);
			return jwtClaims;
		}
		catch (InvalidJwtException e)
		{
			
			// Programmatic access to (some) specific reasons for JWT invalidity is also possible
			// should you want different error handling behavior for certain conditions.

			// Whether or not the JWT has expired being one common reason for invalidity
			if (e.hasExpired())
			{
				try {
					log.warn("JWT expired at " + e.getJwtContext().getJwtClaims().getExpirationTime());
				} catch (MalformedClaimException e1) {
					e1.printStackTrace();
				}
			} else {
				// InvalidJwtException will be thrown, if the JWT failed processing or validation in anyway.
				// Hopefully with meaningful explanations(s) about what went wrong.
				log.warn("Invalid JWT! " + e);

			}
			
			return null;

		}
	}


}
