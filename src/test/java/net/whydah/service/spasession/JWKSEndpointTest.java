package net.whydah.service.spasession;

import static com.jayway.restassured.RestAssured.given;

import java.net.HttpURLConnection;

import org.testng.annotations.Test;

import net.whydah.demoservice.testsupport.AbstractEndpointTest;
import net.whydah.service.auth.JwksEndpointController;
import static org.hamcrest.core.StringContains.containsString;

public class JWKSEndpointTest extends AbstractEndpointTest {

    @Test
    public void testJWKS() {
        given()
                .when()
                .port(getServerPort())
                .get(JwksEndpointController.JWKS_PATH)
                .then().log().ifValidationFails()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body(containsString("keys"))
                .body(containsString("\"kty\":\"RSA\""))
                .body(containsString("kid"))
                .body(containsString("n"))
                .body(containsString("e"))
                ;
    }
}
