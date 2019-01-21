package net.whydah.service.httpproxy.inn;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.StringContains.containsString;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

import java.util.UUID;

import javax.ws.rs.core.Response;

import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.testng.annotations.Test;

import io.restassured.response.ValidatableResponse;

import net.whydah.demoservice.testsupport.AbstractEndpointTest;

public class UserDeliveryAddressResourceTest extends AbstractEndpointTest {

    @Test
    public void getSharedDeliveryAddressWithASecretAndJWT() {
    	String secret = extractTheSecret();
    	String jwt = authenticateAndgetUserToken(secret);
    	
        String apiPath = "/api/" +secret + "/get_shared_delivery_address/";
        given()
              .when().header("AUTHORIZATION", "Bearer " + jwt)
              .port(getServerPort())
              .get(apiPath)
              .then().log().ifValidationFails()
              .statusCode(Response.Status.OK.getStatusCode())
                // The wiremock returns the invoked path for verification
              .body(containsString("shared-delivery-address"));
    }
    
    private String authenticateAndgetUserToken(String secret) {
    	  ValidatableResponse validatableResponse = given()
                  .when()
                  .port(getServerPort())
                  .post("/api/" + secret + "/get_token_from_ticket/" + UUID.randomUUID().toString())
                  .then().log().ifError()
                  .statusCode(Response.Status.OK.getStatusCode());
    	 return validatableResponse.extract().body().asString();
    }
    
    private String extractTheSecret() {
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
        return sessionSecret;
    }
}