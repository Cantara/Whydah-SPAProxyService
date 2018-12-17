package net.whydah.service.auth.ssologin;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.service.SPAApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Repository
class SSOLoginRepository {

    private static final Logger log = LoggerFactory.getLogger(SSOLoginRepository.class);


    private Map<UUID, SSOLoginSession> ssoLoginSessionMap;

    SSOLoginRepository() {
        initializeHazelcast();
        createCleanupThread(600, 60, 120);
    }

    SSOLoginRepository(int maxAgeSeconds,
                       int delaySeconds,
                       int initialDelaySeconds) {
        initializeHazelcast();
        createCleanupThread(maxAgeSeconds, delaySeconds, initialDelaySeconds);
    }

    SSOLoginSession put(UUID ssoLoginUUID, SSOLoginSession ssoLoginSession) {
        return ssoLoginSessionMap.put(ssoLoginUUID, ssoLoginSession);
    }

    SSOLoginSession get(UUID ssoLoginUUID) {
        return ssoLoginSessionMap.get(ssoLoginUUID);
    }

    SSOLoginSession remove(UUID ssoLoginUUID) {
        return ssoLoginSessionMap.remove(ssoLoginUUID);
    }

    /**
     * Initializes a hazelcast ssoLoginSessionMap.
     * Uses the hazelcast xml file provided through -Dhazelcast.config.
     * If the property is not provided the hazelcast.xml from classpath is selected.
     */
    private void initializeHazelcast() {
        String xmlFileName = System.getProperty("hazelcast.config");
        log.info("Loading hazelcast configuration from :" + xmlFileName);
        Config hazelcastConfig = new Config();


        if (xmlFileName != null && xmlFileName.length() > 10) {
            try {
                hazelcastConfig = new XmlConfigBuilder(xmlFileName).build();
                log.info("Loading hazelcast configuration from :" + xmlFileName);
            } catch (FileNotFoundException notFound) {
                log.error("Error - not able to load hazelcast.xml configuration.  Using embedded configuration as fallback");
            }
        } else {
            log.warn("Unable to load external hazelcast.xml configuration.  Using configuration from classpath as fallback");
            InputStream configFromClassPath = SPAApplicationRepository.class.getClassLoader().getResourceAsStream("hazelcast.xml");
            hazelcastConfig = new XmlConfigBuilder(configFromClassPath).build();
        }

        hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");
        //hazelcastConfig.getGroupConfig().setName("OID_HAZELCAST");
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        String gridPrefix = "SPAPROXY";
        ssoLoginSessionMap = hazelcastInstance.getMap(gridPrefix + "_userloginSessions");
        log.info("Connecting to ssoLoginSessionMap {}", gridPrefix + "_userloginSessions");
    }

    /**
     * Creates a thread that deletes entries in the hazelcastMap older than timeUnit maxAgeInSeconds.
     *
     * @param maxAgeInSeconds Entries older than maxAgeInSeconds timeUnits will be removed from the map
     */
    private void createCleanupThread(int maxAgeInSeconds, int delayInSeconds, int initialDelayInSeconds) {
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(2);
        //schedule to run after sometime
        try {
            log.info("Creating cleanup thread that deletes SSOLoginSessions older than {} seconds.", maxAgeInSeconds);
            scheduledThreadPool.scheduleWithFixedDelay(() -> deleteOldIncompleteSessions(maxAgeInSeconds), initialDelayInSeconds, delayInSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error or interrupted trying to process createCleanupThread in SSOLoginRepository", e);
        }
    }

    private void deleteOldIncompleteSessions(int maxAgeInSeconds) {
        for (Map.Entry<UUID, SSOLoginSession> entry : ssoLoginSessionMap.entrySet()) {
            ZonedDateTime initializedTimestamp = entry.getValue().getInitializedTimestamp();

            boolean before = initializedTimestamp.isBefore(ZonedDateTime.now().minus(maxAgeInSeconds, ChronoUnit.SECONDS));

            if (before) {
                log.info("Removed ssoLoginSession with old initializedTimestamp: {}, uuid: {}", initializedTimestamp.toString(), entry.getKey());
                ssoLoginSessionMap.remove(entry.getKey());
            }
        }
    }
}
