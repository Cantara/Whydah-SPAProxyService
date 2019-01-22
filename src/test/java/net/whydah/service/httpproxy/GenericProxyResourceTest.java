package net.whydah.service.httpproxy;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.ValidatableResponse;
import net.whydah.demoservice.testsupport.AbstractEndpointTest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

public class GenericProxyResourceTest extends AbstractEndpointTest {
    private String validSecret;
    private String invalidSecret;
    private String validJwt;

    @BeforeClass
    public void setup() {
        // Extract sessionSecret from a load for the application
        validSecret = loadApplicationSecret("testApp");
        invalidSecret = "invalidSecret";
        validJwt = authenticateAndGetJWT(validSecret);
    }

    @Test //TODO: Replace with another testcase
    public void getSharedDeliveryAddress() {
        String apiPath = "/httpproxy/generic/{secret}/{userTokenId}/{proxySpecificationName}"
                .replace("{secret}", validSecret)
                .replace("{userTokenId}", "TODO")
                .replace("{proxySpecificationName}", "ssolwa-shared-delivery-address");
        ExtractableResponse<io.restassured.response.Response> response = given()
                .when()
                .port(getServerPort())
                .accept("application/json")
                .get(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Status.OK.getStatusCode())
                .extract();


        assertTrue(response.body().asString().contains("shared-delivery-address"));
    }

    @Test //TODO: Replace with another testcase
    public void getSharedDeliveryAddress_JWT() {
        String apiPath = "/httpproxy/generic/{secret}/{proxySpecificationName}"
                .replace("{secret}", validSecret)
                .replace("{proxySpecificationName}", "ssolwa-shared-delivery-address");
        ExtractableResponse<io.restassured.response.Response> response = given()
                .when()
                .port(getServerPort())
                .accept("application/json")
                .header("AUTHORIZATION", "Bearer " + validJwt)
                .get(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Status.OK.getStatusCode())
                .extract();


        assertTrue(response.body().asString().contains("shared-delivery-address"));
    }

    @Test
    public void sts_validate() {
        String apiPath = "/httpproxy/generic/{secret}/{userTokenId}/{proxySpecificationName}"
                .replace("{secret}", validSecret)
                .replace("{userTokenId}", "TODO")
                .replace("{proxySpecificationName}", "sts-validate");
        ExtractableResponse<io.restassured.response.Response> response = given()
                .when()
                .port(getServerPort())
                .accept("application/json")
                .get(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Status.OK.getStatusCode())
                .extract();

        String body = response.body().asString();

        assertEquals(body, "{\"result\": \"true\"}");


    }

    @Test
    public void sts_validate_jwt() {
        String apiPath = "/httpproxy/generic/{secret}/{proxySpecificationName}"
                .replace("{secret}", validSecret)
                .replace("{proxySpecificationName}", "sts-validate");
        ExtractableResponse<io.restassured.response.Response> response = given()
                .when()
                .port(getServerPort())
                .accept("application/json")
                .header("AUTHORIZATION", "Bearer " + validJwt)
                .get(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Status.OK.getStatusCode())
                .extract();

        String body = response.body().asString();

        assertEquals(body, "{\"result\": \"true\"}");
    }

    @Test
    public void get_withoutValidSecret_401() {
        String apiPath = "/httpproxy/generic/{secret}/{userTokenId}/{proxySpecificationName}"
                .replace("{secret}", invalidSecret)
                .replace("{userTokenId}", "irrelevant")
                .replace("{proxySpecificationName}", "irrelevant");
        given()
                .when()
                .port(getServerPort())
                .accept("application/json")
                .get(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Status.FORBIDDEN.getStatusCode())
                .extract();
    }

    @Test
    public void get_validSecret_unkown_proxySpecification_404() {
        String apiPath = "/httpproxy/generic/{secret}/{userTokenId}/{proxySpecificationName}"
                .replace("{secret}", validSecret)
                .replace("{userTokenId}", "irrelevant")
                .replace("{proxySpecificationName}", "irrelevant");
        given()
                .when()
                .port(getServerPort())
                .accept("application/json")
                .get(apiPath)
                .then().log().ifValidationFails()
                .statusCode(Status.NOT_FOUND.getStatusCode())
                .extract();
    }


    private String loadApplicationSecret(String applicationName) {
        ValidatableResponse validatableResponse = given()
                .when()
                .port(getServerPort())
                .redirects().follow(false) //Do not follow the redirect
                .get("/load/" + applicationName)
                .then().log().ifError()
                .statusCode(Status.FOUND.getStatusCode());

        String locationHeader = validatableResponse.extract().header("Location");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(locationHeader).build().getQueryParams();
        String secret = queryParams.get("code").get(0);
        assertNotNull(secret);
        assertFalse(secret.isEmpty());
        return secret;
    }

    private String authenticateAndGetJWT(String secret) {
        ValidatableResponse validatableResponse = given()
                .when()
                .port(getServerPort())
                .post("/api/" + secret + "/get_token_from_ticket/" + UUID.randomUUID().toString())
                .then().log().ifError()
                .statusCode(Response.Status.OK.getStatusCode());
        return validatableResponse.extract().body().asString();
    }

}