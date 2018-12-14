package net.whydah.service.auth.ssologin;

import com.jayway.restassured.response.ValidatableResponse;
import net.whydah.demoservice.testsupport.AbstractEndpointTest;
import net.whydah.util.Configuration;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 *
 */
public class SSOLoginResourceTest extends AbstractEndpointTest {

    @Test
    public void whenInitializeUserLogin_appName_isNotFound_404Returned() {
        String apiPath = "/application/appThatDoesNotExist/user/auth/ssologin/";
        given()
                .when()
                .port(getServerPort())
                .post(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

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
    public void verifyInitializeUserLoginWithAppName() {
        String apiPath = "/application/testApp/user/auth/ssologin/";
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
    public void whenRedirectUserLogin_appName_isNotFound_404Returned() {
        String apiPath = "/application/appThatDoesNotExist/user/auth/ssologin/" + UUID.randomUUID().toString();
        given()
                .when()
                .port(getServerPort())
                .get(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
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
    public void whenRedirectUserLogin_ssoLoginUUID_isNotFound_404Returned() {
        String apiPath = "/application/appName/user/auth/ssologin/" + UUID.randomUUID().toString();
        given()
                .when()
                .port(getServerPort())
                .get(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void verifyRedirectInitializedUserLoginWithAppName() {
        String apiPath = "/application/testApp/user/auth/ssologin/";
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
                "/application/testApp/user/auth/ssologin/" + ssoLoginUUID + "/finalize";

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("appName", "testApp")
                .queryParam("ssoLoginUUID", ssoLoginUUID)
                .build()
                .toString();

        assertEquals(location, expectedLocation);
    }

    @Test
    public void whenRedirectUserLogin_UserCheckoutIsForwarded_inLocation() {
        String apiPath = "/application/testApp/user/auth/ssologin/";
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
                .get(ssoLoginUrl + "?UserCheckout=true") //Add UserCheckout query param
                .then().log().ifValidationFails()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String location = redirectResponse.extract().header("Location");

        assertNotNull(location);
        assertFalse(location.isEmpty());

        String expectedRedirectURI = Configuration.getString("myuri") +
                "/application/testApp/user/auth/ssologin/" + ssoLoginUUID + "/finalize";

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("UserCheckout", "true")
                .queryParam("appName", "testApp")
                .queryParam("ssoLoginUUID", ssoLoginUUID)
                .build()
                .toString();

        assertEquals(location, expectedLocation);
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
                "/application/testApp/user/auth/ssologin/" + ssoLoginUUID + "/finalize";

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("appName", testAppName)
                .queryParam("ssoLoginUUID", ssoLoginUUID)
                .build()
                .toString();

        assertEquals(location, expectedLocation);
    }


    @Test
    public void verifyFinalizeInitializedUserLoginWithSecret() {
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
                "/application/testApp/user/auth/ssologin/" + ssoLoginUUID + "/finalize";

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("appName", testAppName)
                .queryParam("ssoLoginUUID", ssoLoginUUID)
                .build()
                .toString();

        assertEquals(location, expectedLocation);

        ValidatableResponse finalizeResponse = given()
                .when()
                .redirects().follow(false)
                .queryParam("userticket", "testUserTicket")
                .get(expectedRedirectURI)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String expectedFinalizeLocation = "http://dummy.url.does.not.exist.com";
        String actualFinalizeLocation = finalizeResponse.extract().header("Location");

        assertEquals(actualFinalizeLocation, expectedFinalizeLocation);
    }


    @Test
    public void verifyFinalizeInitializedUserLoginWithoutSecret() {
        final String testAppName = "testApp";

        // Initialize the user login
        String apiPath = "/application/" + testAppName +  "/user/auth/ssologin/";
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
                "/application/" +testAppName + "/user/auth/ssologin/" + ssoLoginUUID + "/finalize";

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("appName", testAppName)
                .queryParam("ssoLoginUUID", ssoLoginUUID)
                .build()
                .toString();

        assertEquals(location, expectedLocation);

        ValidatableResponse finalizeResponse = given()
                .when()
                .redirects().follow(false)
                .queryParam("userticket", "testUserTicket")
                .get(expectedRedirectURI)
                .then().log().ifValidationFails()
                .statusCode(Response.Status.FOUND.getStatusCode());

        String expectedFinalizeLocation = Configuration.getString("myuri") + "/load/" + testAppName;
        String actualFinalizeLocation = finalizeResponse.extract().header("Location");

        assertEquals(actualFinalizeLocation, expectedFinalizeLocation);
    }



}