package net.whydah.commands;

import net.whydah.demoservice.testsupport.TestServer;
import net.whydah.service.proxy.ProxyResource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.*;

public class CommandGetProxyResponseTest {
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
    public void testProxyRespurce() throws IOException {
        CommandGetProxyResponse commandGetProxyResponse = new CommandGetProxyResponse(testServer.getUrl()+"/");
        String response =commandGetProxyResponse.execute();
        System.out.println(response);
    }
}
