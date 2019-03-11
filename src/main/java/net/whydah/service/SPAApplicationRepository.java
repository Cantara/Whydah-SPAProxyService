package net.whydah.service;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.appauth.CommandRenewApplicationSession;
import net.whydah.sso.util.backoff.ExponentialBackOff;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

@Repository
public class SPAApplicationRepository {
    private static final Logger log = getLogger(SPAApplicationRepository.class);
    private final CredentialStore credentialStore;
    public static final int APPLICATION_SESSION_CHECK_INTERVAL_IN_SECONDS = 10; 
    private Map<String, ApplicationToken> map;
    private static boolean isRunning = false;

    @Autowired
    public SPAApplicationRepository(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;

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
        map = hazelcastInstance.getMap(gridPrefix + "_applicationTokenMap");
        log.info("Connecting to map {}", gridPrefix + "_applicationTokenMap");

        this.credentialStore.getWas().updateApplinks();

        startProcessWorker();
    }

    public ApplicationToken getApplicationTokenBySecret(String secret) {
        ApplicationToken token = null;
        if (secret != null && !secret.isEmpty()) {
            token = map.get(secret);
        }
        return token;
    }

    public void add(String secret, ApplicationToken applicationToken) {
        map.put(secret, applicationToken);
    }

    public boolean isEmpty() {
        if (map == null || map.size() < 1) {
            return true;
        }
        return false;
    }

    public Map<String, ApplicationToken> allSessions() {
        if (map == null) {
            map = new HashMap<>();
        }
        return map;
    }

    private void startProcessWorker() {
        if (!isRunning) {
            ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(2);
            //schedule to run after sometime
            log.debug("startProcessWorker - Current Time = " + new Date());

            try {
                Thread.sleep(10000);  // Do not start too early...
                scheduledThreadPool.scheduleWithFixedDelay(this::renewApplicationSessions, 0, 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Error or interrupted trying to process dataflow from Proactor", e);
                isRunning = false;
            }
        }
    }

    private void renewApplicationSessions() {
        //Use a set to avoid renewing the same token multiple times   	
    	Map<String, ApplicationToken> sessions = new HashMap<>(allSessions());
        for (ApplicationToken applicationToken : sessions.values()) {
        	if(expiresBeforeNextSchedule(Long.parseLong(applicationToken.getExpires()))) {
        		CommandRenewApplicationSession commandRenewApplicationSession = new CommandRenewApplicationSession(
        				URI.create(credentialStore.getWas().getSTS()), applicationToken.getApplicationTokenId());
        		commandRenewApplicationSession.execute();
        	}
        }
    }
    
    public boolean expiresBeforeNextSchedule(Long timestamp) {

		long currentTime = System.currentTimeMillis();
		long expiresAt = (timestamp);
		long diffSeconds = (expiresAt - currentTime) / 1000;
		log.debug("expiresBeforeNextSchedule - expiresAt: {} - now: {} - expires in: {} seconds", expiresAt, currentTime, diffSeconds);
		if (diffSeconds < (APPLICATION_SESSION_CHECK_INTERVAL_IN_SECONDS * 3)) {
			log.debug("expiresBeforeNextSchedule - re-new application session.. diffseconds: {}", diffSeconds);
			return true;
		}
		return false;
	}
}
