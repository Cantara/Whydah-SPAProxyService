package net.whydah.service.proxy;

import net.whydah.demoservice.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;

import static com.jayway.restassured.RestAssured.given;

public class ProxyResourceTest {
    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer();
        testServer.start();
        Thread.sleep(2000);
    }

    @AfterClass
    public void stop() {
        testServer.stop();
    }

    @Test(enabled = false)
    public void testProxy() {
        given()
                .log()
                .everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_OK)
                .log()
                .everything()
                .when()
                .get(ProxyResource.PROXY_PATH);
    }
}
