package net.whydah.service;

import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.appauth.CommandRenewApplicationSession;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

@Repository
public class SPAApplicationRepository {
    private static final Logger log = getLogger(SPAApplicationRepository.class);
    private final CredentialStore credentialStore;

    private Map<String, ApplicationToken> map;
    private static boolean isRunning = false;

    @Autowired
    public SPAApplicationRepository(CredentialStore credentialStore){
        this.credentialStore=credentialStore;
      
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


    public void add(String secret,ApplicationToken applicationToken){
        map.put(secret,applicationToken);
    }

    public boolean isEmpty() {
        if (map == null || map.size() < 1) {
            return true;
        }
        return false;
    }


    public Collection<ApplicationToken> allSessions() {
        if (map == null) {
            map = new HashMap<>();
        }
        return map.values();
    }

    private void startProcessWorker() {
        if (!isRunning) {

            ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(2);
            //schedule to run after sometime
            log.debug("startProcessWorker - Current Time = " + new Date());
            try {
                Thread.sleep(10000);  // Do not start too early...
                scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
                    public void run() {
                        renewApplicationSessions();
                    }
                }, 0, 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Error or interrupted trying to process dataflow from Proactor", e);
                isRunning = false;
            }

        }
    }

    private void renewApplicationSessions(){
        for (ApplicationToken applicationToken:allSessions()){

            CommandRenewApplicationSession commandRenewApplicationSession= new CommandRenewApplicationSession(URI.create(credentialStore.getWas().getSTS()),applicationToken.getApplicationTokenId());
            commandRenewApplicationSession.execute();
        }

    }
}
