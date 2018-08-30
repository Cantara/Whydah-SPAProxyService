package net.whydah.service.bootstrapflow;

import net.whydah.commands.CommandAPIUserLoginToJWT;
import net.whydah.commands.CommandGetProxyResponse;
import net.whydah.commands.CommandResolveTicketToJWT;
import net.whydah.demoservice.testsupport.TestServer;
import net.whydah.service.authapi.UserAuthenticationAPIResource;
import net.whydah.service.proxy.ProxyResource;
import net.whydah.sso.basehelpers.JsonPathHelper;
import net.whydah.sso.user.types.UserCredential;
import net.whydah.util.Configuration;
import net.whydah.util.StringXORer;
import org.apache.commons.codec.binary.Base64;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class BootstrapFlowTest {

    private TestServer testServer;

    private String secret;
    private String TEST_APPLICATION_NAME = "ACS";

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer();
        testServer.start();

        Thread.sleep(5000);

        CommandGetProxyResponse commandGetProxyResponse = new CommandGetProxyResponse(testServer.getUrl() + ProxyResource.PROXY_PATH + "/" + TEST_APPLICATION_NAME);
        String response = commandGetProxyResponse.execute();
        System.out.println(response);

        if (response != null) {
            String secretA = JsonPathHelper.findJsonPathValue(response, "$.code");
            String secretB = JsonPathHelper.findJsonPathValue(response, "$.cookievalue");
            secret = StringXORer.encode(secretA, secretB);
        } else {
            stop();
        }
    }

    @AfterClass
    public void stop() {
        testServer.stop();
    }

    enum JWTokenPart {
        header,
        payload,
        signature
    }

    private String parseJWT(String jwtToken, JWTokenPart partToReturn) {
        String[] split_string = jwtToken.split("\\.");
        String base64EncodedHeader = split_string[0];
        String base64EncodedBody = split_string[1];
        String base64EncodedSignature = split_string[2];

        System.out.println("~~~~~~~~~ JWT Header ~~~~~~~");
        Base64 base64Url = new Base64(true);
        String header = new String(base64Url.decode(base64EncodedHeader));
        System.out.println("JWT Header : " + header);


        System.out.println("~~~~~~~~~ JWT Body ~~~~~~~");
        String body = new String(base64Url.decode(base64EncodedBody));
        System.out.println("JWT Body : " + body);

        System.out.println("~~~~~~~~~ JWT Signature ~~~~~~~");
        String signature = new String(base64Url.decode(base64EncodedSignature));
        System.out.println("JWT Signature : " + signature);

        if (partToReturn == JWTokenPart.header) {
            return header;
        } else if (partToReturn == JWTokenPart.payload) {
            return body;
        } else if (partToReturn == JWTokenPart.signature) {
            return signature;
        }
        return "";
    }

    @Test(enabled = false) //TODO verify new api verify ticket endpoint
    public void testResolveTicket() {
        UserCredential userCredential = new UserCredential();
        userCredential.setUserName(Configuration.getString("adminuserid"));
        userCredential.setPassword(Configuration.getString("adminusersecret"));

        //log on
        CommandAPIUserLoginToJWT commandAPIUserLoginToJWT = new CommandAPIUserLoginToJWT(testServer.getUrl() + UserAuthenticationAPIResource.API_PATH, secret, userCredential.getUserName(), userCredential.getPassword());
        String response2 = commandAPIUserLoginToJWT.execute();
        assertTrue(response2 != null);

        //parse JWT to get the ticket
        String body = parseJWT(response2, JWTokenPart.payload);
        assertTrue(body != null);
        System.out.println(body);
        String userticket = JsonPathHelper.findJsonPathValue(body, "$.userticket");

        CommandResolveTicketToJWT commandResolveTicketToJWT = new CommandResolveTicketToJWT(testServer.getUrl() + UserAuthenticationAPIResource.API_PATH, secret, userticket, "{}");
        response2 = commandResolveTicketToJWT.execute();
        assertTrue(response2 != null);
        System.out.println(response2);

        //parse JWT to get the ticket
        body = parseJWT(response2, JWTokenPart.payload);
        assertTrue(body != null);
        System.out.println(body);
        String userticket2 = JsonPathHelper.findJsonPathValue(body, "$.userticket");
        //userticket must be renewed
        assertTrue(userticket2 != null && !userticket.equals(userticket2));
    }

    @Test(enabled = false) //TODO verify new api verify ticket endpoint
    public void testAPILogon() {
        UserCredential userCredential = new UserCredential();
        userCredential.setUserName(Configuration.getString("adminuserid"));
        userCredential.setPassword(Configuration.getString("adminusersecret"));
        //CommandAPIUserLoginToJWT commandAPIUserLoginToJWT = new CommandAPIUserLoginToJWT("http://localhost:9898/proxy"+ UserAuthenticationAPIResource.API_PATH,secret, UserCredentialMapper.toXML(userCredential));
        CommandAPIUserLoginToJWT commandAPIUserLoginToJWT = new CommandAPIUserLoginToJWT(testServer.getUrl() + UserAuthenticationAPIResource.API_PATH, secret, userCredential.getUserName(), userCredential.getPassword());
        String response2 = commandAPIUserLoginToJWT.execute();
        assertTrue(response2 != null);

        System.out.println(response2);
    }
}
