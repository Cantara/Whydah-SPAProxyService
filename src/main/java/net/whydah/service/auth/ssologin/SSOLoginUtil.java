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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
            log.debug("Redirecting user to: {}", location);
            return Response.status(Response.Status.FOUND)
                    .header("Location", locationWithOriginalQueryParams.toString())
                    .build();
        }

        log.error("ssoLoginRedirectUrl called with null values. ssoLoginUrl:{}, spaProxyUrl:{}, application:{} ",
                ssoLoginUrl, spaProxyUrl, application.getName());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();

    }

    static Optional<Response> verifySSOLoginSession(SSOLoginSession ssoLoginSession, Application application,
                                                    UUID ssoLoginUUID, SessionStatus expectedStatus) {
        if (ssoLoginSession == null) {
            log.info("redirectInitializedUserLoginWithApplicationSession called with unknown ssoLoginUUID. ssoLoginUUID: {}", ssoLoginUUID);
            return Optional.of(Response.status(Response.Status.NOT_FOUND).build());
        }

        if (!application.getName().equals(ssoLoginSession.getApplicationName())) {
            log.info("redirectInitializedUserLoginWithApplicationSession called with application that does not match ssoLoginSession. " +
                    "Returning forbidden. ssoLoginUUID: {}, appName: {}", ssoLoginUUID, application.getName());
            return Optional.of(Response.status(Response.Status.FORBIDDEN).build());
        }

        if (!expectedStatus.equals(ssoLoginSession.getStatus())) {
            log.info("redirectInitializedUserLogin called with ssoLoginSession with incorrect status. " +
                            "Returning forbidden. ssoLoginUUID: {}, expectedStatus: {}, ssoLoginSession.status: {}",
                    ssoLoginUUID, expectedStatus, ssoLoginSession.getStatus());
            return Optional.of(Response.status(Response.Status.FORBIDDEN).build());
        }

        return Optional.empty();
    }


    static Map<String, String[]> buildQueryParamsForRedirectUrl(
            UUID ssoLoginUUID, final Application application, final Map<String, String[]> originalParameterMap) {

        Map<String, String[]> forwardedParameterMap = new HashMap<>();
        for (Map.Entry<String, String[]> entry : originalParameterMap.entrySet()) {
            forwardedParameterMap.put(entry.getKey(), entry.getValue());
        }
        // Pass through query parameters
        // Overwrite the appName and ssoLoginSession parameter explicitly
        forwardedParameterMap.put("appName", new String[]{application.getName()});
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
}
