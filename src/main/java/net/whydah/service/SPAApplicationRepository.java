package net.whydah.service;

import net.whydah.sso.application.types.ApplicationToken;
import org.slf4j.Logger;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

@Repository
public class SPAApplicationRepository {
    private static final Logger log = getLogger(SPAApplicationRepository.class);

    private Map<String, ApplicationToken> map = new HashMap<>();


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
}
