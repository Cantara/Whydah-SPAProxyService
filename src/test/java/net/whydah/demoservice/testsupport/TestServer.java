package net.whydah.demoservice.testsupport;

import io.restassured.RestAssured;
import net.whydah.service.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServer {
    private static final Logger log = LoggerFactory.getLogger(TestServer.class);

    private final int port;
    private Main main;

    public TestServer(int port) {
        this.port = port;
    }

    public TestServer() {
        this.port = DynamicPortUtil.findAvailableTcpPort();
    }

    public void start() throws Exception {
        new Thread(() -> {
            main = new Main().withPort(port);
            main.start();
        }).start();
        do {
            Thread.sleep(10);
        } while (main == null || !main.isStarted());
        RestAssured.port = main.getPort();
        log.info("Starting TestServer on Port:" + port);
        RestAssured.basePath = Main.CONTEXT_PATH;
    }

    public void stop() {
        main.stop();
    }

    public String getUrl() {
        return "http://localhost:" + main.getPort() + Main.CONTEXT_PATH;
    }
}
