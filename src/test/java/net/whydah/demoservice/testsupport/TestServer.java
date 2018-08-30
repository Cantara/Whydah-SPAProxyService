package net.whydah.demoservice.testsupport;

import com.jayway.restassured.RestAssured;
import net.whydah.service.Main;

public class TestServer {
    private Main main;

    public TestServer() {
    }

    public void start() throws InterruptedException {
        new Thread(() -> {
            main = new Main();
            main.start();
        }).start();
        do {
            Thread.sleep(10);
        } while (main == null || !main.isStarted());
        RestAssured.port = main.getPort();

        RestAssured.basePath = Main.CONTEXT_PATH;
        String url = "http://localhost:" + main.getPort() + Main.CONTEXT_PATH;
    }

    public void stop() {
        main.stop();
    }

    public String getUrl() {
        return "http://localhost:" + main.getPort() + Main.CONTEXT_PATH;
    }
}
