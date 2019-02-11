package net.whydah.testsupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import net.whydah.service.Main;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.time.Instant;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * This class sets up a Wiremock server, stubs needed endpoints in STS and UAT to start this application, configures the
 * application to use these endpoints and start the application itself. Dynamic ports are used.
 */
public abstract class AbstractEndpointTest {
    private static TestServer testServer;
    private static int serverPort;
    private static WireMockServer wireMockServer;

    private static String testServerBaseUrl;

    @BeforeSuite(alwaysRun = true, timeOut = 30000L)
    public void startTestServer() throws Exception {
        serverPort = DynamicPortUtil.findAvailableTcpPort();
        setupWiremock();
        testServer = new TestServer(serverPort);
        testServerBaseUrl = "http://localhost:" + serverPort + Main.CONTEXT_PATH;
        System.setProperty("myuri", testServerBaseUrl);
        System.setProperty("proxy.specification.load.from.classpath", "true");
        System.setProperty("proxy.specification.directory", "proxy-specifications");
        testServer.start();
    }

    @AfterSuite(alwaysRun = true)
    public void stop() {
        testServer.stop();
    }

    @BeforeMethod(alwaysRun = true)
    public void setupCommonStubs() {
        setupExternalServicesMocks();
    }


    private void setupWiremock() {
        wireMockServer = new WireMockServer(options()
                .dynamicPort()
                .extensions(new ResponseTemplateTransformer(false))
        );
        wireMockServer.start();
        setupExternalServicesMocks();
        int wiremockPort = wireMockServer.port();

        System.setProperty("securitytokenservice", "http://localhost:" + wiremockPort + "/tokenservice/");
        System.setProperty("useradminservice", "http://localhost:" + wiremockPort + "/useradminservice/");
        System.setProperty("logonservice", "http://localhost:" + wiremockPort + "/oidsso/");

        System.setProperty("jetty.request.log.enabled", "false");

    }

    /**
     * Mocks required for successful service start
     * TODO: Might need to return more sensible results or expose values to subclasses.
     */
    private void setupExternalServicesMocks() {
        // Initial logon - STS
        addStub(WireMock.post(urlEqualTo("/tokenservice/logon"))
                .withRequestBody(WireMock.matching(".*402222.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(" <applicationtoken>\n" +
                                "     <params>\n" +
                                "         <applicationtokenID>86560f039fcfbb083bed8c12da58bdee</applicationtokenID>\n" +
                                "         <applicationid>402222</applicationid>\n" +
                                "         <applicationname>Whydah-SPAProxyService</applicationname>\n" +
                                "         <expires>" + (Instant.now().toEpochMilli() / 1000) + "</expires>\n" +
                                "     </params> \n" +
                                "     <Url type=\"application/xml\" method=\"POST\"                 template=\"http://localhost:" + wireMockServer.port() + "/tokenservice/user/86560f039fcfbb083bed8c12da58bdee/get_usertoken_by_usertokenid\"/> \n" +
                                " </applicationtoken>\n"))
        );

        // testApp logon
        addStub(WireMock.post(urlEqualTo("/tokenservice/logon"))
                .withRequestBody(WireMock.matching(".*inMemoryTestAppId.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(" <applicationtoken>\n" +
                                "     <params>\n" +
                                "         <applicationtokenID>12340f039fcfbb083bed8c12da581234</applicationtokenID>\n" +
                                "         <applicationid>inMemoryTestAppId</applicationid>\n" +
                                "         <applicationname>testApp</applicationname>\n" +
                                "         <expires>" + (Instant.now().toEpochMilli() / 1000) + "</expires>\n" +
                                "     </params> \n" +
                                "     <Url type=\"application/xml\" method=\"POST\"                 template=\"http://localhost:" + wireMockServer.port() + "/tokenservice/user/86560f039fcfbb083bed8c12da58bdee/get_usertoken_by_usertokenid\"/> \n" +
                                " </applicationtoken>\n"))
        );

        // validate logon - STS
        addStub(WireMock.get(urlEqualTo("/tokenservice/86560f039fcfbb083bed8c12da58bdee/validate"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("{\"result\": \"true\"}")
                )
        );

        // validate logon - STS - inMemoryTestAppId
        addStub(WireMock.get(urlEqualTo("/tokenservice/12340f039fcfbb083bed8c12da581234/validate"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("{\"result\": \"true\"}")
                )
        );


        // get applications from UAS. Return single testApp
        Application application = new Application("inMemoryTestAppId", "testApp");
        application.setApplicationUrl("http://dummy.url.does.not.exist.com");
        application.setTags("ALLOWEDQUERYPARAMS_code;firstName;lastName;streetAddress;someExtraQueryParam;");
        addStub(WireMock.get(urlEqualTo("/useradminservice/86560f039fcfbb083bed8c12da58bdee/applications"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(ApplicationMapper.toJson(Collections.singletonList(application))))
        );

        // Get specific application from UAS: testApp
        addStub(WireMock.get(urlMatching("/useradminservice/86560f039fcfbb083bed8c12da58bdee/.*/application/inMemoryTestAppId"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(ApplicationMapper.toJson(application)))
        );

        // get application key - STS
        addStub(WireMock.get(urlEqualTo("/tokenservice/86560f039fcfbb083bed8c12da58bdee/get_application_key"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("{\"encryptionKey\":\"hHwfdaOnBfROLhmQUUPTZLwBNm5PWtxx4P9ny3A34jU=\",\n" +
                                "\"iv\":\"MDEyMzQ1Njc4OTBBQkNERQ==\"}"))
        );



        addStub(WireMock.get(urlMatching("/oidsso/.*/api/.*/shared-delivery-address"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{{request.requestLine.path}}")
                        .withTransformers("response-template")
                ));

        // get a stub-usertoken by userticket
        UserToken testAppClientUserToken = new UserToken();
        testAppClientUserToken.setUserName("testAppUser");
        testAppClientUserToken.setLastName("AbstractEndpointTest");
        testAppClientUserToken.setUid("09876543210987654321");
        testAppClientUserToken.setUserTokenId("testAppUserTokenId1234");
        addStub(WireMock.post(urlMatching("/tokenservice/user/12340f039fcfbb083bed8c12da581234/get_usertoken_by_userticket"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(UserTokenMapper.toXML(testAppClientUserToken))
                )
        );

        addStub(WireMock.post(urlMatching("/tokenservice/user/12340f039fcfbb083bed8c12da581234/create_userticket_by_usertokenid"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(UserTokenMapper.toXML(testAppClientUserToken))
                )
        );

        addStub(WireMock.get(urlMatching("/oidsso/health"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reason\":  \"ok\"}")
                )
        );

        addStub(WireMock.post(urlMatching("/tokenservice/user/.*/.*/usertoken"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(UserTokenMapper.toXML(testAppClientUserToken))
                )
        );

        // validate logon - STS - inMemoryTestAppId
        addStub(WireMock.get(urlEqualTo("/tokenservice/user/12340f039fcfbb083bed8c12da581234/validate_usertokenid/testAppUserTokenId1234"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("{\"result\": \"true\"}")
                )
        );


        // A stub which simply returns the request body in the response.
        addStub(WireMock.post(urlMatching("/tokenservice/.*/api/.*/parrot"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{{request.body}}")
                        .withTransformers("response-template")
                ));

        //
    }

    private static void addStub(MappingBuilder stub) {
        wireMockServer.stubFor(stub);
    }

    /**
     * Add a stub for a STS or UAS call. Will be cleaned between each test
     *
     * @param stub the stub of type {@link MappingBuilder} to add
     */
    public static void addWiremockStub(MappingBuilder stub) {
        addStub(stub);
    }

    public static int getWireMockPort() {
        return wireMockServer.port();
    }

    /**
     * @return The dynamic port the test server is running on
     */
    protected static int getServerPort() {
        return serverPort;
    }

    /**
     * @return The base url of the test server, including dynamic port and context path
     */
    protected static String getBaseUrl() {
        return testServerBaseUrl;
    }
}
