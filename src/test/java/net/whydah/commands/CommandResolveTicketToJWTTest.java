package net.whydah.commands;

import net.whydah.demoservice.testsupport.TestServer;
import net.whydah.service.authapi.UserAuthenticationAPIResource;
import net.whydah.service.proxy.ProxyResource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.*;

public class CommandResolveTicketToJWTTest {
    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
        testServer.start();
        Thread.sleep(3000);

    }

    @AfterClass
    public void stop() {
        testServer.stop();
    }

    @Test //TODO verify new api endpoint
    public void testResolveTicket() throws IOException {
        CommandResolveTicketToJWT commandResolveTicketToJWT = new CommandResolveTicketToJWT(testServer.getUrl()+ UserAuthenticationAPIResource.API_PATH,"secret","ticket","{}");
        String response =commandResolveTicketToJWT.execute();
        System.out.println(response);
    }
}