package net.whydah.service.auth;

import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

// The jose.4.j library is an open source (Apache 2.0) implementation of JWT and the JOSE specification suite.
// It is written in Java and relies solely on the JCA APIs for cryptography.
//
// JSON Web Token (JWT) is a compact, URL-safe means of representing claims to be transferred between two parties.
// JWT is the identity token format in OpenID Connect and it is also widely used in OAuth 2.0 and many other contexts that require compact message security.
//
// The JWT code examples page shows how to easily produce and consume JWTs using this library.
//
// JOSE is short for Javascript Object Signing and Encryption, which is the IETF Working Group that developed the JSON Web Signature (JWS),
// JSON Web Encryption (JWE) and JSON Web Key (JWK) specifications. JWS and JWE use JSON and base64url encoding to secure messages in a (relatively) simple,
// compact and web safe format while JWK defines a JSON representation of cryptographic keys.
// The actual algorithms for JWS, JWE and JWK are defined in JSON Web Algorithms (JWA).
//
// The library supports the JWS/JWE compact serializations with the complete suite of JOSE algorithms.
// A more detailed breakdown is available below in the Algorithm Support section.
public final class AdvancedJWTokenUtil {
    private static final Logger log = LoggerFactory.getLogger(AdvancedJWTokenUtil.class);

    private AdvancedJWTokenUtil() {
    }

    public static String buildJWT(RsaJsonWebKey rsaJsonWebKey, UserToken usertoken, String userTicket, String applicationId) {
        log.debug("RSA hash code... " + rsaJsonWebKey.hashCode());
        JwtClaims claims = new JwtClaims();
        claims.setSubject(usertoken.getUserName()); // the subject/principal is whom the token is about
        claims.setJwtId(usertoken.getUserTokenId());
        claims.setIssuer(usertoken.getIssuer());
        claims.setAudience("");// to whom the token is intended to be sent
        claims.setIssuedAtToNow();
        if (userTicket != null) {
            claims.setClaim("userticket", userTicket);
        }

        // Filter on application id as an additional guard even though STS should for normal use cases only return a single application
        List<UserApplicationRoleEntry> unmappedRoles = usertoken.getRoleList()
                .stream()
                .filter(role -> applicationId != null && applicationId.equalsIgnoreCase(role.getApplicationId()))
                .collect(Collectors.toList());
        unmappedRoles.stream().findFirst().ifPresent(role -> {
                    claims.setClaim("applicationName", role.getApplicationName());
                    claims.setClaim("applicationId", role.getApplicationId());
                }
        );

        List<Map<String, String>> mappedRoles = unmappedRoles.stream()
                .map(role -> {
                    Map<String, String> rolesMap = new HashMap<>();
                    ofNullable(role.getOrgName()).ifPresent(orgName -> rolesMap.put("orgName", orgName));
                    ofNullable(role.getRoleName()).ifPresent(roleName -> rolesMap.put("roleName", roleName));
                    ofNullable(role.getRoleValue()).ifPresent(roleValue -> rolesMap.put("roleValue", roleValue));
                    return rolesMap;
                })
                .collect(Collectors.toList());
        claims.setClaim("roles", mappedRoles);

        //add expiration date
        NumericDate numericDate = NumericDate.now();
        numericDate.addSeconds(Long.parseLong(usertoken.getLifespan()) / 1000);
        claims.setExpirationTime(numericDate);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId());
        jws.setKey(rsaJsonWebKey.getPrivateKey());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        String jwt = null;
        try {
            jwt = jws.getCompactSerialization();
        } catch (JoseException ex) {
            log.error("failed to generate JWT");
        }

        log.info("Claim:\n" + claims);
        log.info("JWS:\n" + jws);
        log.info("JWT:\n" + jwt);
        return jwt;
    }

    static JwtClaims parseJWT(String jwt, Key key) {
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
                .setVerificationKey(key) // verify the signature with the public key
                .setExpectedAudience(false, "")
                .setJwsAlgorithmConstraints( // only allow the expected signature algorithm(s) in the given context
                        new AlgorithmConstraints(ConstraintType.WHITELIST, // which is only RS256 here
                                AlgorithmIdentifiers.RSA_USING_SHA256))
                .build(); // create the JwtConsumer instance

        try {
            //  Validate the JWT and process it to the Claims
            JwtClaims jwtClaims = jwtConsumer.processToClaims(jwt);
            log.info("JWT validation succeeded! " + jwtClaims);
            return jwtClaims;
        } catch (InvalidJwtException e) {
            // Programmatic access to (some) specific reasons for JWT invalidity is also possible
            // should you want different error handling behavior for certain conditions.

            // Whether or not the JWT has expired being one common reason for invalidity
            if (e.hasExpired()) {
                try {
                    log.warn("JWT expired at " + e.getJwtContext().getJwtClaims().getExpirationTime());
                } catch (MalformedClaimException e1) {
                    log.error("MalformedClaimException caught: ", e);
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
