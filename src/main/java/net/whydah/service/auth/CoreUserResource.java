package net.whydah.service.auth;

import net.whydah.service.CredentialStore;
import net.whydah.sso.application.types.Application;
import net.whydah.util.Configuration;

import javax.ws.rs.core.Response;

abstract class CoreUserResource {
    public static final String API_PATH = "/api";

    static final String logonUrl = Configuration.getString("logonservice");

    final CredentialStore credentialStore;

    CoreUserResource(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    Response createResponseWithHeader(String data, String applicationName) {
        Application application = credentialStore.findApplication(applicationName);
        String redirectUrl = credentialStore.findRedirectUrl(application);

        return Response.ok(data)
                .header("Access-Control-Allow-Origin", redirectUrl)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }

    Response createForbiddenResponseWithHeader(String applicationName) {
        Application application = credentialStore.findApplication(applicationName);
        String redirectUrl = credentialStore.findRedirectUrl(application);

        return Response.status(Response.Status.FORBIDDEN)
                .header("Access-Control-Allow-Origin", redirectUrl)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }
}
