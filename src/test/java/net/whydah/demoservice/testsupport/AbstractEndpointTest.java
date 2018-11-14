package net.whydah.demoservice.testsupport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * This class sets up a Wiremock server, stubs needed endpoints in STS and UAT to start this application, configures the
 * application to use these endpoints and start the application itself. Dynamic ports are used.
 */
public abstract class AbstractEndpointTest {
    private static TestServer testServer;
    private static int serverPort;
    private static WireMockServer wireMockServer;

    @BeforeSuite(alwaysRun = true, timeOut = 30000L)
    public void startTestServer() throws Exception {
        serverPort = DynamicPortUtil.findAvailableTcpPort();
        setupWiremock();
        testServer = new TestServer(serverPort);
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

    @AfterMethod(alwaysRun = true)
    public void cleanupStubs() {
        wireMockServer.resetAll();
    }

    private void setupWiremock() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        int wiremockPort = wireMockServer.port();

        System.setProperty("securitytokenservice", "http://localhost:" + wiremockPort + "/tokenservice/");
        System.setProperty("useradminservice", "http://localhost:" + wiremockPort + "/useradminservice/");
        System.setProperty("jetty.request.log.enabled", "false");

        setupExternalServicesMocks();
    }

    /**
     * Mocks required for successful service start
     * TODO: Might need to return more sensible results or expose values to subclasses.
     */
    private void setupExternalServicesMocks() {
        // Initial logon - STS
        addStub(WireMock.post(urlEqualTo("/tokenservice/logon"))
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

        // validate logon - STS
        addStub(WireMock.get(urlEqualTo("/tokenservice/86560f039fcfbb083bed8c12da58bdee/validate"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("{\"result\": \"true\"}")
                )
        );

        // get applications from UAS. Return empty list
        addStub(WireMock.get(urlEqualTo("/useradminservice/86560f039fcfbb083bed8c12da58bdee/applications"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("[]"))
        );

        // get application key - STS
        addStub(WireMock.get(urlEqualTo("/tokenservice/86560f039fcfbb083bed8c12da58bdee/get_application_key"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("{\"encryptionKey\":\"hHwfdaOnBfROLhmQUUPTZLwBNm5PWtxx4P9ny3A34jU=\",\n" +
                                "\"iv\":\"MDEyMzQ1Njc4OTBBQkNERQ==\"}"))
        );
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

    /**
     * @return The dynamic port the test server is running on
     */
    public static int getServerPort() {
        return serverPort;
    }
}
