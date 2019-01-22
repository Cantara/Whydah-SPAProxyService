package net.whydah.service.httpproxy;

import net.whydah.demoservice.testsupport.FileUtil;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import static org.testng.Assert.*;

public class ProxySpecificationRepositoryTest {
    @Test
    public void verify_ProxySpecificationsFromClassPath() throws IOException {
        ProxySpecificationRepository repository = new ProxySpecificationRepository(
                "proxy-specifications", true);

        Optional<ProxySpecification> proxySpecification = repository.get("sts-validate");
        assertTrue(proxySpecification.isPresent());
    }

    @Test
    public void verify_ProxySpecificationsFromDirectory() throws IOException {
        if (!Files.isDirectory(Paths.get("proxy-specifications/"))) {
            Files.createDirectory(Paths.get("proxy-specifications"));
        }
        String stsValidate = FileUtil.readFile("proxy-specifications/sts-validate.json");
        Files.write(Paths.get("proxy-specifications/written-to-file.json"), Collections.singletonList(stsValidate), Charset.forName("UTF-8"));

        ProxySpecificationRepository repository = new ProxySpecificationRepository(
                "proxy-specifications", false);

        Optional<ProxySpecification> proxySpecification = repository.get("written-to-file");
        assertTrue(proxySpecification.isPresent());

        Files.deleteIfExists(Paths.get("proxy-specifications/written-to-file.json"));
    }

}