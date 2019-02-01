package net.whydah.service.auth.ssologin;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.ValidatableResponse;
import net.whydah.testsupport.AbstractEndpointTest;
import net.whydah.util.Configuration;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * This test class verifies {@link SSOLoginResource} flow when session secret is previously provisioned
 */
public class SSOLoginResourceWithSessionTest extends AbstractEndpointTest {
    @Test
    public void whenInitializeUserLogin_sessionSecret_isNotFound_401Returned() {
        String apiPath = "/application/session/sessionSecretDoesNotExist/user/auth/ssologin/";
        given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void whenInitializeUserLogin_sessionSecret_isMisMatch_401Returned() {
        // Extract sessionSecret1 from a load for the application
        ValidatableResponse validatableResponse = given()
                .when()
                .port(getServerPort())
                .redirects().follow(false) //Do not follow the redirect
                .get("/load/testApp")
                .then().log().ifError()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String locationHeader = validatableResponse.extract().header("Location");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(locationHeader).build().getQueryParams();
        String sessionSecret1 = queryParams.get("code").get(0);
        assertNotNull(sessionSecret1);
        assertFalse(sessionSecret1.isEmpty());


        String apiPath = "/application/session/" + sessionSecret1 + "/user/auth/ssologin/";
        ValidatableResponse response = given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.OK.getStatusCode());


        String ssoLoginUrl = response.extract().path("ssoLoginUrl");
        String ssoLoginUUID = response.extract().path("ssoLoginUUID");
        assertEquals(ssoLoginUrl, getBaseUrl() + apiPath + ssoLoginUUID);

        // Throws exception if it does not conform with UUID format
        UUID uuid = UUID.fromString(ssoLoginUUID);
        assertNotNull(uuid);


        // Continue with the same ssoLoginUUID, but another (valid) sessionSecret
        // Extract sessionSecret1 from a load for the application
        ValidatableResponse validatableResponse2 = given()
                .when()
                .port(getServerPort())
                .redirects().follow(false) //Do not follow the redirect
                .get("/load/testApp")
                .then().log().ifError()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String locationHeader2 = validatableResponse2.extract().header("Location");
        String sessionSecret2 = UriComponentsBuilder.fromUriString(locationHeader2).build().getQueryParams().getFirst("code");
        assertNotNull(sessionSecret2);
        assertFalse(sessionSecret2.isEmpty());


        String incorrectLoginUrl = ssoLoginUrl.replace(sessionSecret1, sessionSecret2);

        given()
                .when()
                .get(incorrectLoginUrl)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.FORBIDDEN.getStatusCode());

    }

    @Test
    public void verifyInitializeUserLoginWithSecret() {
        // Extract sessionSecret from a load for the application
        ValidatableResponse validatableResponse = given()
                .when()
                .port(getServerPort())
                .redirects().follow(false) //Do not follow the redirect
                .get("/load/testApp")
                .then().log().ifError()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String locationHeader = validatableResponse.extract().header("Location");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(locationHeader).build().getQueryParams();
        String sessionSecret = queryParams.get("code").get(0);
        assertNotNull(sessionSecret);
        assertFalse(sessionSecret.isEmpty());


        // Initialize the user login
        String apiPath = "/application/session/" + sessionSecret + "/user/auth/ssologin/";
        ValidatableResponse response = given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.OK.getStatusCode());
        String ssoLoginUrl = response.extract().path("ssoLoginUrl");
        String ssoLoginUUID = response.extract().path("ssoLoginUUID");
        assertEquals(ssoLoginUrl, getBaseUrl() + apiPath + ssoLoginUUID);

        // Throws exception if it does not conform with UUID format
        UUID uuid = UUID.fromString(ssoLoginUUID);
        assertNotNull(uuid);
    }

    @Test
    public void whenRedirectUserLogin_sessionSecret_isNotFound_401Returned() {
        String apiPath = "/application/session/sessionSecretDoesNotExist/user/auth/ssologin/" + UUID.randomUUID().toString();
        given()
                .when()
                .port(getServerPort())
                .get(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    public void verifyRedirectInitializedUserLoginWithSecret() {
        final String testAppName = "testApp";

        // Extract secret from a load for the application
        ValidatableResponse validatableResponse = given()
                .when()
                .port(getServerPort())
                .redirects().follow(false) //Do not follow the redirect
                .get("/load/" + testAppName)
                .then().log().ifError()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String locationHeader = validatableResponse.extract().header("Location");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(locationHeader).build().getQueryParams();
        String secret = queryParams.get("code").get(0);
        assertNotNull(secret);
        assertFalse(secret.isEmpty());


        // Initialize the user login
        String apiPath = "/application/session/" + secret + "/user/auth/ssologin/";
        ValidatableResponse initResponse = given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.OK.getStatusCode());
        String ssoLoginUrl = initResponse.extract().path("ssoLoginUrl");
        String ssoLoginUUID = initResponse.extract().path("ssoLoginUUID");

        ValidatableResponse redirectResponse = given()
                .when()
                .redirects().follow(false) //Do not follow the redirect
                .get(ssoLoginUrl)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String location = redirectResponse.extract().header("Location");

        assertNotNull(location);
        assertFalse(location.isEmpty());

        String expectedRedirectURI = Configuration.getString("myuri") +
                "/application/testApp/user/auth/ssologin/" + ssoLoginUUID + "/complete";

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("ssoLoginUUID", ssoLoginUUID)
                .queryParam("targetApplicationId", "inMemoryTestAppId")
                .build()
                .toString();

        assertEquals(location, expectedLocation);
    }

    @Test
    public void verifyCompleteInitializedUserLoginWithSecret() {
        final String testAppName = "testApp";

        // Extract secret from a load for the application
        ValidatableResponse validatableResponse = given()
                .when()
                .port(getServerPort())
                .redirects().follow(false) //Do not follow the redirect
                .get("/load/" + testAppName)
                .then().log().ifError()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String locationHeader = validatableResponse.extract().header("Location");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(locationHeader).build().getQueryParams();
        String secret = queryParams.get("code").get(0);
        assertNotNull(secret);
        assertFalse(secret.isEmpty());


        // Initialize the user login
        String apiPath = "/application/session/" + secret + "/user/auth/ssologin/";
        ValidatableResponse initResponse = given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.OK.getStatusCode());
        String ssoLoginUrl = initResponse.extract().path("ssoLoginUrl");
        String ssoLoginUUID = initResponse.extract().path("ssoLoginUUID");

        ValidatableResponse redirectResponse = given()
                .when()
                .redirects().follow(false) //Do not follow the redirect
                .get(ssoLoginUrl)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String location = redirectResponse.extract().header("Location");

        assertNotNull(location);
        assertFalse(location.isEmpty());

        String expectedRedirectURI = Configuration.getString("myuri") +
                "/application/testApp/user/auth/ssologin/" + ssoLoginUUID + "/complete";

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("ssoLoginUUID", ssoLoginUUID)
                .queryParam("targetApplicationId", "inMemoryTestAppId")
                .build()
                .toString();

        assertEquals(location, expectedLocation);

        ValidatableResponse completeResponse = given()
                .when()
                .redirects().follow(false)
                .queryParam("userticket", "testUserTicket")
                .get(expectedRedirectURI)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String expectedCompleteLocation = "http://dummy.url.does.not.exist.com";
        String actualCompleteLocation = completeResponse.extract().header("Location");

        assertEquals(actualCompleteLocation, expectedCompleteLocation);

        ExtractableResponse<io.restassured.response.Response> jwtResponse = given()
                .when()
                .redirects().follow(false)
                .post("/application/session/" + secret + "/user/auth/ssologin/" + ssoLoginUUID + "/exchange-for-token")
                .then().log().ifValidationFails()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract();

        String jwt = jwtResponse.body().asString();
        assertNotNull(jwt);
        assertFalse(jwt.isEmpty());
    }

    @Test
    public void verifyQueryParamRedirect() {
        final String testAppName = "testApp";

        // Extract secret from a load for the application
        ValidatableResponse validatableResponse = given()
                .when()
                .port(getServerPort())
                .redirects().follow(false) //Do not follow the redirect
                .get("/load/" + testAppName)
                .then().log().ifError()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String locationHeader = validatableResponse.extract().header("Location");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(locationHeader).build().getQueryParams();
        String secret = queryParams.get("code").get(0);
        assertNotNull(secret);
        assertFalse(secret.isEmpty());


        // Initialize the user login
        String apiPath = "/application/session/" + secret + "/user/auth/ssologin/";
        ValidatableResponse initResponse = given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.OK.getStatusCode());
        String ssoLoginUrl = initResponse.extract().path("ssoLoginUrl");
        String ssoLoginUUID = initResponse.extract().path("ssoLoginUUID");

        ValidatableResponse redirectResponse = given()
                .when()
                .redirects().follow(false) //Do not follow the redirect
                .get(ssoLoginUrl)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String location = redirectResponse.extract().header("Location");

        assertNotNull(location);
        assertFalse(location.isEmpty());

        String expectedRedirectURI = Configuration.getString("myuri") +
                "/application/testApp/user/auth/ssologin/" + ssoLoginUUID + "/complete";

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("ssoLoginUUID", ssoLoginUUID)
                .queryParam("targetApplicationId", "inMemoryTestAppId")
                .build()
                .toString();

        assertEquals(location, expectedLocation);
    }

}
