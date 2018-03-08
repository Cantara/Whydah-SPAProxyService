package net.whydah.service.bootstrapflow;

import net.whydah.commands.CommandAPIUserLoginToJWT;
import net.whydah.commands.CommandGetProxyResponse;
import net.whydah.commands.CommandResolveTicketToJWT;
import net.whydah.demoservice.testsupport.TestServer;
import net.whydah.service.authapi.UserAuthenticationAPIResource;
import net.whydah.service.proxy.ProxyResource;
import net.whydah.sso.basehelpers.JsonPathHelper;
import net.whydah.sso.user.mappers.UserCredentialMapper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.util.Configuration;
import net.whydah.util.StringXORer;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class BootstrapFlowTest {
    private TestServer testServer;
    private StringXORer stringXORer= new StringXORer();
    String secret;
    String TEST_APPLICATION_NAME = "Whydah-Jenkins";
    
    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
        testServer.start();
        
        Thread.sleep(5000);
        
        CommandGetProxyResponse commandGetProxyResponse = new CommandGetProxyResponse(testServer.getUrl()+ ProxyResource.PROXY_PATH+"/" + TEST_APPLICATION_NAME);
        String response =commandGetProxyResponse.execute();
        System.out.println(response);

        if(response!=null){
        	String secretA = JsonPathHelper.findJsonPathValue(response,"$.code");
        	String secretB = JsonPathHelper.findJsonPathValue(response,"$.cookievalue");
        	secret = stringXORer.encode(secretA,secretB);
        } else {
        	stop();
        }
    }

    @AfterClass
    public void stop() {
        testServer.stop();
    }

    @Test //TODO verify new api verify ticket endpoint
    public void testResolveTicket() throws IOException {


        CommandResolveTicketToJWT commandResolveTicketToJWT = new CommandResolveTicketToJWT(testServer.getUrl()+ UserAuthenticationAPIResource.API_PATH,secret,"ticket","{}");
        String response2 =commandResolveTicketToJWT.execute();
        System.out.println(response2);
    }

    @Test //TODO verify new api verify ticket endpoint
    public void testAPILogon() throws IOException {


        UserCredential userCredential= new UserCredential();
        userCredential.setUserName(Configuration.getString("adminuserid"));
        userCredential.setPassword(Configuration.getString("adminusersecret"));

        CommandAPIUserLoginToJWT commandAPIUserLoginToJWT = new CommandAPIUserLoginToJWT(testServer.getUrl()+ UserAuthenticationAPIResource.API_PATH,secret, UserCredentialMapper.toXML(userCredential));
        String response2 =commandAPIUserLoginToJWT.execute();
        System.out.println(response2);
    }
}
