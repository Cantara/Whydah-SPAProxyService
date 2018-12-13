package net.whydah.service.auth.ssologin;

import net.whydah.sso.application.types.Application;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 *
 */
public class SSOLoginUtilTest {

    @Test
    public void testInitializeSSOLoginResponse() {
        URI uri = URI.create("http://localhost/unittest");
        UUID ssoLoginUUID = UUID.randomUUID();
        String applicationUrl = "https://used.for.cors.headers";


        Response response = SSOLoginUtil.initializeSSOLoginResponse(uri, ssoLoginUUID, applicationUrl);

        assertEquals(response.getStatus(), 200);
        assertEquals(response.getHeaderString("Access-Control-Allow-Origin"), applicationUrl);
        assertEquals(response.getHeaderString("Access-Control-Allow-Credentials"), "true");
        assertEquals(response.getHeaderString("Access-Control-Allow-Headers"), "*");
    }

    @Test
    public void testVerifySSOLoginSession() {
        UUID ssoLoginUUID = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");
        SSOLoginSession ssoLoginSession = new SSOLoginSession(ssoLoginUUID, SSOLoginResource.INITILIZED_VALUE, application.getName());

        Optional<Response> response = SSOLoginUtil.verifySSOLoginSession(ssoLoginSession, application, ssoLoginUUID);

        assertFalse(response.isPresent());

    }

    @Test
    public void verifySSOLoginSession_SessionIsNull_404_Response() {
        UUID ssoLoginUUID = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");

        Optional<Response> optionalResponse = SSOLoginUtil.verifySSOLoginSession(null, application, ssoLoginUUID);

        assertTrue(optionalResponse.isPresent());
        Response response = optionalResponse.get();

        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void verifySSOLoginSession_applicationMismatch_403_Response() {
        UUID ssoLoginUUID = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");
        Application misMatch = new Application("error", "error");
        SSOLoginSession ssoLoginSession = new SSOLoginSession(ssoLoginUUID, SSOLoginResource.INITILIZED_VALUE, application.getName());

        Optional<Response> optionalResponse = SSOLoginUtil.verifySSOLoginSession(ssoLoginSession, misMatch, ssoLoginUUID);

        assertTrue(optionalResponse.isPresent());
        Response response = optionalResponse.get();

        assertEquals(response.getStatus(), 403);
    }

    @Test
    public void verifySSOLoginSession_SessionStatusMismatch_403_Response() {
        UUID ssoLoginUUID = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");
        SSOLoginSession ssoLoginSession = new SSOLoginSession(ssoLoginUUID, "incorrect", application.getName());

        Optional<Response> optionalResponse = SSOLoginUtil.verifySSOLoginSession(ssoLoginSession, application, ssoLoginUUID);

        assertTrue(optionalResponse.isPresent());
        Response response = optionalResponse.get();

        assertEquals(response.getStatus(), 403);
    }

    @Test
    public void testBuildQueryParamsForRedirectUrl() {
        UUID uuid = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");

        Map<String, String[]> originalQueryParams = Collections.singletonMap("testQuery", new String[] {"true"});

        Map<String, String[]> newQueryParams = SSOLoginUtil.buildQueryParamsForRedirectUrl(uuid, application, originalQueryParams);

        assertEquals( newQueryParams.size(), 3);
        assertEquals(newQueryParams.get("testQuery"), new String[]{"true"});
        assertEquals(newQueryParams.get("appName"), new String[]{application.getName()});
        assertEquals(newQueryParams.get("ssoLoginUUID"), new String[]{uuid.toString()});
    }
}