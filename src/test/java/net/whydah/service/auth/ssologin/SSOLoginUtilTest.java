package net.whydah.service.auth.ssologin;

import net.whydah.sso.application.types.Application;
import org.apache.http.client.utils.URIBuilder;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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
    public void test_verifySSOLoginSessionWithoutSessionSecret() {
        UUID ssoLoginUUID = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");

        SessionStatus expectedStatus = SessionStatus.INITIALIZED;
        SSOLoginSession ssoLoginSession = new SSOLoginSession(ssoLoginUUID, expectedStatus, application.getName());

        Optional<Response> response = SSOLoginUtil.verifySSOLoginSessionWithoutSessionSecret(
                ssoLoginSession, application, ssoLoginUUID, expectedStatus);

        assertFalse(response.isPresent());
    }

    @Test
    public void test_verifySSOLoginSessionWithSessionSecret_MatchingSessionHash() {
        UUID ssoLoginUUID = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");

        SessionStatus expectedStatus = SessionStatus.INITIALIZED;
        SSOLoginSession ssoLoginSession = new SSOLoginSession(ssoLoginUUID, expectedStatus, application.getName(), "hashHere");

        Optional<Response> response = SSOLoginUtil.verifySSOLoginSessionWithSessionSecret(
                ssoLoginSession, application, ssoLoginUUID, expectedStatus, "hashHere");

        assertFalse(response.isPresent());
    }

    @Test
    public void test_verifySSOLoginSessionWithoutSessionSecret_SessionIsNull_404_Response() {
        UUID ssoLoginUUID = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");

        Optional<Response> optionalResponse = SSOLoginUtil.verifySSOLoginSessionWithoutSessionSecret
                (null, application, ssoLoginUUID, SessionStatus.INITIALIZED);

        assertTrue(optionalResponse.isPresent());
        Response response = optionalResponse.get();

        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void verifySSOLoginSessionWithSessionSecret_SessionIsNull_404_Response() {
        UUID ssoLoginUUID = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");

        Optional<Response> optionalResponse = SSOLoginUtil.verifySSOLoginSessionWithSessionSecret
                (null, application, ssoLoginUUID, SessionStatus.INITIALIZED, "irrelevant");

        assertTrue(optionalResponse.isPresent());
        Response response = optionalResponse.get();

        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void verifySSOLoginSessionWithoutSessionSecret_applicationMismatch_403_Response() {
        UUID ssoLoginUUID = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");
        Application misMatch = new Application("error", "error");
        SessionStatus expectedStatus = SessionStatus.INITIALIZED;

        SSOLoginSession ssoLoginSession = new SSOLoginSession(ssoLoginUUID, expectedStatus, application.getName());

        Optional<Response> optionalResponse = SSOLoginUtil.verifySSOLoginSessionWithoutSessionSecret(
                ssoLoginSession, misMatch, ssoLoginUUID, expectedStatus);

        assertTrue(optionalResponse.isPresent());
        Response response = optionalResponse.get();

        assertEquals(response.getStatus(), 403);
    }

    @Test
    public void verifySSOLoginSessionWithoutSessionSecret_SessionStatusMismatch_403_Response() {
        UUID ssoLoginUUID = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");
        SessionStatus expectedStatus = SessionStatus.INITIALIZED;
        SSOLoginSession ssoLoginSession = new SSOLoginSession(ssoLoginUUID, expectedStatus, application.getName());

        SessionStatus actualStatus = SessionStatus.REDIRECTED;

        Optional<Response> optionalResponse = SSOLoginUtil.verifySSOLoginSessionWithoutSessionSecret(
                ssoLoginSession, application, ssoLoginUUID, actualStatus);

        assertTrue(optionalResponse.isPresent());
        Response response = optionalResponse.get();

        assertEquals(response.getStatus(), 403);
    }

    @Test
    public void verifySSOLoginSessionWithSessionSecret_SessionSecretMismatch_403_Response() {
        UUID ssoLoginUUID = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");
        SessionStatus expectedStatus = SessionStatus.INITIALIZED;
        SSOLoginSession ssoLoginSession = new SSOLoginSession(ssoLoginUUID, expectedStatus, application.getName(), "expectedSessionHash");

        SessionStatus actualStatus = SessionStatus.REDIRECTED;

        Optional<Response> optionalResponse = SSOLoginUtil.verifySSOLoginSessionWithSessionSecret(
                ssoLoginSession, application, ssoLoginUUID, actualStatus, "actualSessionHash");

        assertTrue(optionalResponse.isPresent());
        Response response = optionalResponse.get();

        assertEquals(response.getStatus(), 403);
    }

    @Test
    public void testBuildQueryParamsForRedirectUrl() {
        UUID uuid = UUID.randomUUID();
        Application application = new Application("unitTestId", "unitTest");

        Map<String, String[]> originalQueryParams = Collections.singletonMap("testQuery", new String[]{"true"});

        Map<String, String[]> newQueryParams = SSOLoginUtil.buildQueryParamsForRedirectUrl(uuid, application, originalQueryParams);

        assertEquals(newQueryParams.size(), 3);
        assertEquals(newQueryParams.get("testQuery"), new String[]{"true"});
        assertEquals(newQueryParams.get("targetApplicationId"), new String[]{application.getId()});
        assertEquals(newQueryParams.get("ssoLoginUUID"), new String[]{uuid.toString()});
    }

    @Test
    public void test_buildPopupEntryPointURIWithApplicationSession() {
        String spaProxyBaseURI = "https://localhost/spaproxy";
        String sessionSecret = "sessionSecretValue";
        UUID ssoLoginUUID = UUID.randomUUID();

        URI uri = SSOLoginUtil.buildPopupEntryPointURIWithApplicationSession(spaProxyBaseURI, sessionSecret, ssoLoginUUID);

        assertEquals(uri, URI.create("https://localhost/spaproxy/application/session/sessionSecretValue/user/auth/ssologin/" + ssoLoginUUID.toString()));

    }

    @Test
    public void test_buildPopupEntryPointURIWithoutApplicationSession() {
        String spaProxyBaseURI = "https://localhost/spaproxy";
        String applicationName = "appName";
        UUID ssoLoginUUID = UUID.randomUUID();

        URI uri = SSOLoginUtil.buildPopupEntryPointURIWithoutApplicationSession(spaProxyBaseURI, applicationName, ssoLoginUUID);

        assertEquals(uri, URI.create("https://localhost/spaproxy/application/appName/user/auth/ssologin/" + ssoLoginUUID.toString()));

    }

    @Test
    public void test_sha256Hash_isIdempotent() throws NoSuchAlgorithmException {
        String hash1 = SSOLoginUtil.sha256Hash("ThisSecret");
        String hash2 = SSOLoginUtil.sha256Hash("ThisSecret");

        assertNotNull(hash1);
        assertFalse(hash1.isEmpty());
        assertEquals(hash1, hash2);
    }

    @Test
    public void test_removeKeysFromMap() {
        Map<String, String[]> originalQueryParamMap = new HashMap<String, String[]>();

        String key1 = "someParam";
        String key2 = "someParam2";
        String[] value1 = {"someValue"};
        String[] value2 = {"someOtherValue"};
        originalQueryParamMap.put(key1, value1);
        originalQueryParamMap.put(key2, value2);

        Map<String, String[]> expectedCleanMap = new HashMap<String, String[]>(originalQueryParamMap);

        String[] paramsToKeep = {key1};
        expectedCleanMap.remove(key2);

        Map<String, String[]> cleanMap = SSOLoginUtil.removeKeysFromMap(paramsToKeep, originalQueryParamMap);

        assertEquals(cleanMap, expectedCleanMap);
    }

    @Test
    public void test_addQueryParamsToUri() {
        Map<String, String[]> queryParams = new HashMap<String, String[]>();
        String key1 = "someParam";
        String key2 = "someParam2";
        String[] value1 = {"someValue"};
        String[] value2 = {"someOtherValue"};
        queryParams.put(key1, value1);
        queryParams.put(key2, value2);

        String mockUri = "http://www.someodmain.com/somewhere";
        UriBuilder mockUriBuilder = UriBuilder.fromUri(mockUri);
        UriBuilder uriBuilderWithParams = SSOLoginUtil.addQueryParamsToUri(queryParams, mockUriBuilder);
        assertNotNull(uriBuilderWithParams);
        assertEquals(uriBuilderWithParams.build().toString(), String.format(mockUri + "?%s=%s&%s=%s", key2, value2[0], key1, value1[0]));

    }
}