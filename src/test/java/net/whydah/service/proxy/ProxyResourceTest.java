package net.whydah.service.proxy;

import net.whydah.demoservice.testsupport.TestServer;
import net.whydah.service.health.HealthResource;
import org.junit.Ignore;
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
        Thread.sleep(2000);

    }

    @AfterClass
    public void stop() {
        testServer.stop();
    }

    @Test
    @Ignore
    public void testProxy() throws IOException {
        given()
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_OK)
                .log().everything()
                .when()
                .get(ProxyResource.PROXY_PATH);
    }
}
