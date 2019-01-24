package net.whydah.service.httpproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
@Repository
public class ProxySpecificationRepository {
    private static final Logger log = LoggerFactory.getLogger(ProxySpecificationRepository.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, ProxySpecification> proxySpecifications;

    @Autowired
    @Configure
    public ProxySpecificationRepository(@Configuration("proxy.specification.directory") String path,
                                        @Configuration("proxy.specification.load.from.classpath") boolean loadFromClasspath) throws IOException {
        this.proxySpecifications = new HashMap<>();

        proxySpecifications.putAll(getSpecificationsFromDisk(path));

        if (loadFromClasspath) {
            proxySpecifications.putAll(getSpecificationsFromClasspath(path));
        }

    }

    private static Map<String, ProxySpecification> getSpecificationsFromDisk(String path) throws IOException {
        File folder = new File(path);
        File[] files = folder.listFiles();
        Map<String, ProxySpecification> specifications = new HashMap<>();
        if (files == null) {
            log.warn("Found no files on disk in {}", path);
            return specifications;
        }
        getProxySpecifications(specifications, files);
        return specifications;
    }

    private static Map<String, ProxySpecification> getSpecificationsFromClasspath(String folder) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Map<String, ProxySpecification> specifications = new HashMap<>();
        URL url = loader.getResource(folder);
        if (url == null) {
            log.warn("Failed to find ProxySpecifications from classpath");
            return specifications;
        }
        String path = url.getPath();
        File[] files = new File(path).listFiles();
        if (files == null) {
            log.warn("Found no files on classpath in {}", path);
            return specifications;
        }
        getProxySpecifications(specifications, files);
        return specifications;
    }

    private static void getProxySpecifications(final Map<String, ProxySpecification> specifications, final File[] files) throws IOException {
        for (File file : files) {
            if (file != null && file.isFile()) {
                ProxySpecification proxySpecification = mapper.readValue(file, ProxySpecification.class);
                specifications.put(file.getName().replace(".json", ""), proxySpecification);
            }
        }
    }

    public Optional<ProxySpecification> get(HttpMethod httpMethod, String proxySpecificationName) {
        return get(httpMethod.name().toLowerCase() +"-"+ proxySpecificationName);
    }

    private Optional<ProxySpecification> get(String proxySpecificationName) {
        return Optional.ofNullable(proxySpecifications.get(proxySpecificationName));

    }

}
