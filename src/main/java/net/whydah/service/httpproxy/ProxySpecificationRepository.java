package net.whydah.service.httpproxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
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
    // public ProxySpecificationRepository(@Configuration("proxy.specification.directory") String directory) throws IOException {
    public ProxySpecificationRepository(@Configuration("logonservice") String logonServiceBaseUrl) throws IOException {
        this.proxySpecifications = new HashMap<>();
        addGetAddressSpecification(logonServiceBaseUrl);
        // getSpecificationsFromPath(directory);

    }

    // TODO: temporary
    private void addGetAddressSpecification(String logonServiceBaseUrl) throws IOException {

        String sharedDeliveryAddress = "{\n" +
                "  \"command_url\" : \""+logonServiceBaseUrl+"#applicationTokenId/api/#userTokenId/shared-delivery-address\",\n" +
                "  \"command_contenttype\" : \"application/json\",\n" +
                "  \"command_http_post\" : false,\n" +
                "  \"command_timeout_milliseconds\" : 5000,\n" +
                "  \"command_template\" : \"\",\n" +
                "  \"command_replacement_map\" : {},\n" +
                "  \"command_response_map\" : {}\n" +
                "}";

        ProxySpecification proxySpecification = mapper.readValue(sharedDeliveryAddress, ProxySpecification.class);

        // String commandUrl = logonServiceBaseUrl + "#applicationTokenId/api/#userTokenId/shared-delivery-address";
        // ProxySpecification sharedDeliveryAddress = new ProxySpecification(commandUrl,
        //         "",
        //         "",
        //         "false",
        //         "true",
        //         "5000",
        //         "",
        //         new HashMap<>(),
        //         new HashMap<>()
        // );
        proxySpecifications.put("ssolwa-shared-delivery-address", proxySpecification);
    }

    //TODO: verify before taking into use
    private void getSpecificationsFromPath(String directory) throws IOException {
        File folder = new File(directory);
        File[] directories = folder.listFiles();
        if (directories == null) {
            log.warn("Found no files in {}. ProxySpecificationRepository is empty", directory);
            return;
        }
        for (File file : directories) {
            if (file != null && file.isFile()) {
                ProxySpecification proxySpecification = mapper.readValue(file, ProxySpecification.class);
                proxySpecifications.put(file.getName(), proxySpecification);
            }

        }
    }

    public Optional<ProxySpecification> get(String proxySpecificationName) {
        return Optional.ofNullable(proxySpecifications.get(proxySpecificationName));

    }

}
