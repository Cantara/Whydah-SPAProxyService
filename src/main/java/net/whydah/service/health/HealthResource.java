package net.whydah.service.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.util.WhydahUtil;
import net.whydah.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;



/**
 * Simple health endpoint for checking the server is running
 *
 * @author <a href="mailto:asbjornwillersrud@gmail.com">Asbjørn Willersrud</a> 30/03/2016.
 */
@Path(HealthResource.HEALTH_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {
    public static final String HEALTH_PATH = "/health";
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;
    static String resultJson = "";
    private static String applicationInstanceName = "";



    @Autowired
    public HealthResource(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository = spaApplicationRepository;
        this.applicationInstanceName = Configuration.getString("applicationname");
    }


    @GET
    public Response healthCheck() {
        log.trace("healthCheck");
        return Response.ok(getHealthTextJson()).build();
    }

    public String getHealthTextJson() {
        return "{\n" +
                "  \"Status\": \"OK\",\n" +
                "  \"Version\": \"" + getVersion() + "\",\n" +
                "  \"DEFCON\": \"" + credentialStore.getWas().getDefcon() + "\",\n" +
                "  \"STS\": \"" + credentialStore.getWas().getSTS() + "\",\n" +
                "  \"UAS\": \"" + credentialStore.getWas().getUAS() + "\",\n" +
                "  \"hasApplicationToken\": \"" + credentialStore.hasApplicationToken() + "\",\n" +
                "  \"hasValidApplicationToken\": \"" + credentialStore.hasValidApplicationToken() + "\",\n" +
                "  \"hasApplicationsMetadata\": \"" + credentialStore.hasApplicationsMetadata() + "\",\n" +
                "  \"ConfiguredApplications\": \"" + credentialStore.getWas().getApplicationList().size() + "\",\n" +

                "  \"now\": \"" + Instant.now()+ "\",\n" +
                "  \"running since\": \"" + WhydahUtil.getRunningSince() + "\",\n\n" +

                "  \"applicationSessionStatistics\": " + getClientIdsJson() + "\n" +
                "}\n";
    }


    private String getVersion() {
        Properties mavenProperties = new Properties();
        String resourcePath = "/META-INF/maven/net.whydah.service/Whydah-OAuth2Service/pom.properties";
        URL mavenVersionResource = this.getClass().getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath) + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "(DEV VERSION)" + " [" + applicationInstanceName + " - " + WhydahUtil.getMyIPAddresssesString() + "]";
    }

    private synchronized String getClientIdsJson() {

        Collection<ApplicationToken> applicationSessions = spaApplicationRepository.allSessions();
        if (applicationSessions == null || applicationSessions.size() < 1) {
            return "\"\"";
        }
        Map<String, Integer> countMap = new HashMap();
        for (ApplicationToken applicationToken : applicationSessions) {
            if (countMap.get(applicationToken.getApplicationName()) == null) {
                countMap.put(applicationToken.getApplicationName(), 1);
            } else {
                countMap.put(applicationToken.getApplicationName(), 1 + countMap.get(applicationToken.getApplicationName()));
            }
        }
        try {
            String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(countMap);

            return jsonString;
        } catch (Exception e) {
            return "\"\"";
        }
    }


}