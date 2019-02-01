package net.whydah.service.auth.ssologin;

import net.whydah.service.CredentialStore;
import net.whydah.service.spasession.ResponseUtil;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUserticket;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 *
 */
class SSOLoginUtil {
    private static final Logger log = LoggerFactory.getLogger(SSOLoginUtil.class);


    static Response initializeSSOLoginResponse(URI ssoLoginUrl, UUID ssoLoginUUID, String applicationUrl) {
        String body = "{ \"ssoLoginUrl\" : \"" + ssoLoginUrl + "\", \"ssoLoginUUID\": \"" + ssoLoginUUID + "\"}";
        return Response.ok(body)
                .header("Access-Control-Allow-Origin", applicationUrl)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }

    static UserToken getUserToken(final CredentialStore credentialStore, final ApplicationToken applicationToken, final String userTicket) {
        CommandGetUsertokenByUserticket commandGetUsertokenByUserticket = new CommandGetUsertokenByUserticket(
                URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId(),
                ApplicationTokenMapper.toXML(applicationToken), userTicket);
        return UserTokenMapper.fromUserTokenXml(commandGetUsertokenByUserticket.execute());
    }

    /**
     * Creates the url the user should be redirected to after successfully authenticating with SSOLWA
     *
     * @param ssoLoginUrl     Base url of SSOLWA
     * @param spaProxyUrl     Base url of this service
     * @param application     Single Page Application of the current SSOLoginSession
     * @param queryParameters Original query parameters sent by the client
     * @param ssoLoginUUID    The UUID of the SSOLoginSession
     * @return A redirect response leading the usert o SSOLWA
     */
    static Response ssoLoginRedirectUrl(String ssoLoginUrl, String spaProxyUrl,
                                        Application application, Map<String, String[]> queryParameters,
                                        UUID ssoLoginUUID) {
        if (ssoLoginUrl != null && spaProxyUrl != null && application != null) {
            URI redirectURI = UriBuilder.fromUri(URI.create(spaProxyUrl))
                    .path(SSOLoginResource.WITHOUT_SESSION_PATH.replace("{appName}", application.getName()))
                    .path(ssoLoginUUID.toString())
                    .path("complete")
                    .build();
            URI location = UriBuilder.fromUri(URI.create(ssoLoginUrl))
                    .path("login")
                    .queryParam("redirectURI", redirectURI)
                    .build();

            URI locationWithOriginalQueryParams = ResponseUtil.addQueryParameters(location, queryParameters);
            log.debug("Redirecting user to: {}", locationWithOriginalQueryParams);
            return Response.status(Response.Status.FOUND)
                    .header("Location", locationWithOriginalQueryParams.toString())
                    .build();
        }

        log.error("ssoLoginRedirectUrl called with null values. ssoLoginUrl:{}, spaProxyUrl:{}, application:{} ",
                ssoLoginUrl, spaProxyUrl, application.getName());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();

    }

    static Optional<Response> verifySSOLoginSessionIgnoreSessionSecret(SSOLoginSession ssoLoginSession, Application application,
                                                                       UUID ssoLoginUUID, SessionStatus expectedStatus) {
        return verifySSOLoginSession(ssoLoginSession, application, ssoLoginUUID, expectedStatus);
    }

    static Optional<Response> verifySSOLoginSessionWithoutSessionSecret(SSOLoginSession ssoLoginSession, Application application,
                                                                        UUID ssoLoginUUID, SessionStatus expectedStatus) {
        return verifySSOLoginSessionWithSessionSecret(ssoLoginSession, application, ssoLoginUUID, expectedStatus, null);
    }

    static Optional<Response> verifySSOLoginSessionWithSessionSecret(SSOLoginSession ssoLoginSession, Application application,
                                                                     UUID ssoLoginUUID, SessionStatus expectedStatus,
                                                                     String expectedSpaSessionSecretHash) {
        if (ssoLoginSession == null) {
            log.info("SSOLoginResource called with unknown ssoLoginUUID. ssoLoginUUID: {}", ssoLoginUUID);
            return Optional.of(Response.status(Response.Status.NOT_FOUND).build());
        }


        if (ssoLoginSession.getSpaSessionSecretHash() != null
                && !ssoLoginSession.getSpaSessionSecretHash().equals(expectedSpaSessionSecretHash)) {
            log.info("SSOLoginResource called with mismatching SpaSessionSecret. " +
                            "Returning forbidden. ssoLoginUUID: {}, spaSessionSecretHash: {}, expectedSpaSessionSecretHash: {}",
                    ssoLoginUUID, ssoLoginSession.getSpaSessionSecretHash(), expectedSpaSessionSecretHash);
            return Optional.of(Response.status(Response.Status.FORBIDDEN).build());
        }

        return verifySSOLoginSession(ssoLoginSession, application, ssoLoginUUID, expectedStatus);
    }

    private static Optional<Response> verifySSOLoginSession(SSOLoginSession ssoLoginSession, Application application,
                                                                             UUID ssoLoginUUID, SessionStatus expectedStatus) {

        if (ssoLoginSession == null) {
            log.info("SSOLoginResource called with unknown ssoLoginUUID. ssoLoginUUID: {}", ssoLoginUUID);
            return Optional.of(Response.status(Response.Status.NOT_FOUND).build());
        }

        if (!application.getName().equals(ssoLoginSession.getApplicationName())) {
            log.info("SSOLoginResource called with application that does not match ssoLoginSession. " +
                    "Returning forbidden. ssoLoginUUID: {}, appName: {}", ssoLoginUUID, application.getName());
            return Optional.of(Response.status(Response.Status.FORBIDDEN).build());
        }

        if (!expectedStatus.equals(ssoLoginSession.getStatus())) {
            log.info("SSOLoginResource called with ssoLoginSession with incorrect status. " +
                            "Returning forbidden. ssoLoginUUID: {}, expectedStatus: {}, ssoLoginSession.status: {}",
                    ssoLoginUUID, expectedStatus, ssoLoginSession.getStatus());
            return Optional.of(Response.status(Response.Status.FORBIDDEN).build());
        }

        return Optional.empty();
    }

    /**
     * addQueryParamsToUri
     * @description Takes an UriBuilder and params, and adds the param set to the URI
     * @param params
     * @param uriBuilder
     * @return uriBuilder with the params
     */
    static UriBuilder addQueryParamsToUri(Map<String, String[]> params, UriBuilder uriBuilder) {
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            uriBuilder.queryParam(entry.getKey(), entry.getValue()[0]);
        }
        return uriBuilder;
    }

    static Map<String, String[]> removeKeysFromMap(String[] paramsToRemove, Map<String, String[]> map) {
        Map<String, String[]> cleanMap = new HashMap<String, String[]>();
        for (Map.Entry<String, String[]> entry: cleanMap.entrySet()) {
            if (Arrays.asList(paramsToRemove).indexOf(entry.getKey()) < 0) {
                cleanMap.put(entry.getKey(), entry.getValue());
            }
        }
        return cleanMap;
    }

    static Map<String, String[]> buildQueryParamsForRedirectUrl(UUID ssoLoginUUID, final Application application,
                                                                final Map<String, String[]> originalParameterMap) {

        Map<String, String[]> forwardedParameterMap = new HashMap<>();
        for (Map.Entry<String, String[]> entry : originalParameterMap.entrySet()) {
            forwardedParameterMap.put(entry.getKey(), entry.getValue());
        }
        // Pass through query parameters
        // Overwrite the appName and ssoLoginSession parameter explicitly
        forwardedParameterMap.put("targetApplicationId", new String[]{application.getId()});
        forwardedParameterMap.put("ssoLoginUUID", new String[]{ssoLoginUUID.toString()});
        return forwardedParameterMap;
    }

    static URI buildPopupEntryPointURIWithApplicationSession(String spaProxyBaseURI, String sessionSecret, UUID ssoLoginUUID) {
        String path = SSOLoginResource.WITH_SESSION_PATH.replace("{spaSessionSecret}", sessionSecret);
        return UriBuilder.fromUri(spaProxyBaseURI)
                .path(path)
                .path(ssoLoginUUID.toString())
                .build();
    }

    static URI buildPopupEntryPointURIWithoutApplicationSession(String spaProxyBaseURI, String appName, UUID ssoLoginUUID) {
        String path = SSOLoginResource.WITHOUT_SESSION_PATH.replace("{appName}", appName);
        return UriBuilder.fromUri(spaProxyBaseURI)
                .path(path)
                .path(ssoLoginUUID.toString())
                .build();
    }

    /**
     * @return A sha256 hash of spaSessionSecret as a hex String
     */
    static String sha256Hash(String spaSessionSecret) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(
                spaSessionSecret.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedHash);
    }


    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte aHash : hash) {
            String hex = Integer.toHexString(0xff & aHash);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
