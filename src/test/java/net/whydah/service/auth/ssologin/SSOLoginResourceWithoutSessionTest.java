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
 * This test class verifies {@link SSOLoginResource} flow when session secret is not previously provisioned
 */
public class SSOLoginResourceWithoutSessionTest extends AbstractEndpointTest {

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
                "/application/testApp/user/auth/ssologin/" + ssoLoginUUID + "/complete";

        String expectedLocation = UriBuilder.fromUri(Configuration.getString("logonservice"))
                .path("login")
                .queryParam("redirectURI", expectedRedirectURI)
                .queryParam("UserCheckout", "true")
                .queryParam("ssoLoginUUID", ssoLoginUUID)
                .queryParam("targetApplicationId", "inMemoryTestAppId")
                .build()
                .toString();

        assertEquals(location, expectedLocation);
    }





    @Test
    public void verifyCompleteInitializedUserLoginWithoutSecret() {
        final String testAppName = "testApp";

        // Initialize the user login
        String apiPath = "/application/" + testAppName + "/user/auth/ssologin/";
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
                "/application/" + testAppName + "/user/auth/ssologin/" + ssoLoginUUID + "/complete";

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

        assertTrue(actualCompleteLocation.startsWith(expectedCompleteLocation));

        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(actualCompleteLocation).build().getQueryParams();
        String secret = queryParams.getFirst("code");

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


}