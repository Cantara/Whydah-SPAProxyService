package net.whydah.service.auth.ssologin;

import net.whydah.sso.application.types.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
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

    static Optional<Response> verifySSOLoginSession(SSOLoginSession ssoLoginSession, Application application, UUID ssoLoginUUID) {
        if (ssoLoginSession == null) {
            log.info("redirectInitializedUserLoginWithApplicationSession called with unknown ssoLoginUUID. ssoLoginUUID: {}", ssoLoginUUID);
            return Optional.of(Response.status(Response.Status.NOT_FOUND).build());
        }

        if (!application.getName().equals(ssoLoginSession.getApplicationName())) {
            log.info("redirectInitializedUserLoginWithApplicationSession called with application that does not match ssoLoginSession. " +
                    "Returning forbidden. ssoLoginUUID: {}, appName: {}", ssoLoginUUID, application.getName());
            return Optional.of(Response.status(Response.Status.FORBIDDEN).build());
        }

        if (!SSOLoginResource.INITILIZED_VALUE.equals(ssoLoginSession.getStatus())) {
            log.info("redirectInitializedUserLogin called with ssoLoginSession with incorrect status. " +
                    "Returning forbidden. ssoLoginUUID: {}, ssoLoginSession.status: {}", ssoLoginUUID, ssoLoginSession.getStatus());
            return Optional.of(Response.status(Response.Status.FORBIDDEN).build());
        }

        return Optional.empty();
    }


    static Map<String, String[]> buildQueryParamsForRedirectUrl(UUID ssoLoginUUID, final Application application, final Map<String, String[]> originalParameterMap) {
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
}
