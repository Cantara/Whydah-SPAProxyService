package net.whydah.service.bootstrapflow;

import net.whydah.commands.CommandGetProxyResponse;
import net.whydah.commands.CommandResolveTicketToJWT;
import net.whydah.demoservice.testsupport.TestServer;
import net.whydah.service.authapi.UserAuthenticationAPIResource;
import net.whydah.service.proxy.ProxyResource;
import net.whydah.sso.basehelpers.JsonPathHelper;
import net.whydah.util.StringXORer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class BootstrapFlowTest {
    private TestServer testServer;
    private StringXORer stringXORer= new StringXORer();


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

        CommandGetProxyResponse commandGetProxyResponse = new CommandGetProxyResponse(testServer.getUrl()+ ProxyResource.PROXY_PATH+"/Whydah-TestWebApplication");
        String response =commandGetProxyResponse.execute();
        System.out.println(response);

        String secretA = JsonPathHelper.findJsonPathValue(response,"$.code");
        String secretB = JsonPathHelper.findJsonPathValue(response,"$.cookievalue");

        String secret = stringXORer.encode(secretA,secretB);

        CommandResolveTicketToJWT commandResolveTicketToJWT = new CommandResolveTicketToJWT(testServer.getUrl()+ UserAuthenticationAPIResource.API_PATH,secret,"ticket","{}");
        String response2 =commandResolveTicketToJWT.execute();
        System.out.println(response2);
    }
}
