package net.whydah.service.proxy;

import net.whydah.demoservice.testsupport.TestServer;
import net.whydah.service.health.HealthResource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.jayway.restassured.RestAssured.given;

public class ProxyResourceTest {

    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
        testServer.start();
        //Thread.sleep(15000);

    }

    @AfterClass
    public void stop() {
        testServer.stop();
    }

    @Test //TODO verify new health test
    public void testHealth() throws IOException {
        given()
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_OK)
                .log().everything()
                .when()
                .get(ProxyResource.PROXY_PATH);
    }
}
