package net.whydah.service.spasession;

import net.whydah.service.CredentialStore;
import net.whydah.sso.application.types.Application;
import net.whydah.util.Configuration;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Map;

/**
 *
 */
public final class ResponseUtil {
    private static final Logger log = LoggerFactory.getLogger(ResponseUtil.class);

    private ResponseUtil() {
    }

    /**
     * Creates a response for the ssoLoginFlow, intended to work with popup-like mode/flow
     * where secret has already been provisioned to SPA
     *
     * @param ssoLoginUrl     URL of SSO Login Webapp
     * @param application     The SPA Application
     * @return A 302-FOUND {@link Response} with Location set to the desired new URL.
     */
    public static Response ssoLoginBackToSpaRedirectUrl(String ssoLoginUrl, Application application,
                                                        Map<String, String[]> queryParameters) {
        if (ssoLoginUrl != null && application != null) {
            URI redirectURI = UriBuilder.fromUri(URI.create(application.getApplicationUrl()))
                    .build();
            URI location = UriBuilder.fromUri(URI.create(ssoLoginUrl))
                    .path("login")
                    .queryParam("redirectURI", redirectURI)
                    .build();

            URI locationWithOriginalQueryParams = ResponseUtil.addQueryParameters(location, queryParameters);
            log.debug("Redirecting user to: {}", locationWithOriginalQueryParams);
            return Response.status(Response.Status.FOUND)
                    .header("Location", location)
                    .build();
        }

        log.error("ssoLoginRedirectUrl called with null values. ssoLoginUrl:{}, application:{} ",
                ssoLoginUrl, application);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Creates a response for the ssoLoginFlow, intended to work with popup-like mode/flow
     * where secret needs to be provisioned after user has logged in.
     *
     * @param ssoLoginUrl     URL of SSO Login Webapp
     * @param spaProxyUrl     URL external users use to access this application
     * @param application     The SPA Application
     * @param queryParameters Query parameters that will be added to the redirect
     * @return A 302-FOUND {@link Response} with Location set to the desired new URL.
     */
    public static Response ssoLoginRedirectUrl(String ssoLoginUrl, String spaProxyUrl,
                                        Application application, Map<String, String[]> queryParameters) {
        if (ssoLoginUrl != null && spaProxyUrl != null && application != null) {
            URI redirectURI = UriBuilder.fromUri(URI.create(spaProxyUrl))
                    .path("load")
                    .path(application.getName())
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

    private static URI addQueryParameters(URI uri, Map<String, String[]> parameters) {
        UriBuilder uriBuilder = UriBuilder.fromUri(uri);

        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            // Remove responseURI if client sends it in. Use redirectURI from application store instead
            if (entry.getValue() != null && entry.getValue().length > 0 && !"redirectURI".equals(entry.getKey())) {
                uriBuilder.queryParam(entry.getKey(), entry.getValue()[0]);
            }
        }

        return uriBuilder.build();
    }

    static Response spaRedirectUrl(CredentialStore credentialStore, Application application,
                                   SPASessionSecret spaSessionSecret, String newTicket) {
        String spaRedirectUrl = credentialStore.findRedirectUrl(application);
        String origin = Configuration.getBoolean("allow.origin") ? "*" : spaRedirectUrl;
        String location = spaRedirectUrl + "?code=" + spaSessionSecret.getSecret() + "&ticket=" + newTicket;
        /*
        String setCookie =
                "code=" + spaSessionSecret.getSecretPart2() +
                        ";expires=" + 846000 +
                        ";path=" + "/" +
                        ";HttpOnly" +
                        ";secure";
        */
        return Response.status(Response.Status.FOUND)
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .header("Access-Control-Expose-Headers", "Cookie")
                .header("Location", location)
                //.header("SET-COOKIE", setCookie)
                .build();
    }

    static Response okResponse(CredentialStore credentialStore, Application application,
                               SPASessionSecret spaSessionSecret, String newTicket) {
        String redirectUrl = credentialStore.findRedirectUrl(application);
        String body = createJSONBody(spaSessionSecret.getSecret(), newTicket).toString();

        return Response.ok(body)
                .header("Access-Control-Allow-Origin", redirectUrl)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
//                .cookie(getCookie(spaSessionSecret.getSecretPart2()))
                .build();
    }

    private static JSONObject createJSONBody(String secret, String ticket) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("secret", secret);
            if (ticket != null) {
                jsonObject.put("ticket", ticket);
            }
        } catch (JSONException e) {
            log.error("JSON object with secret could not be created", e);
        }
        return jsonObject;
    }
}
