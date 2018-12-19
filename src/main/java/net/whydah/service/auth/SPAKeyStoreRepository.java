package net.whydah.service.auth;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;


@Repository
public class SPAKeyStoreRepository {
	private static final Logger logger = getLogger(SPAKeyStoreRepository.class);
	private static String keystoreSource = System.getProperty("user.dir") + "/spa-keystore.jwks";
	
	private Map<String, JsonWebKey> map;
	private JsonWebKeySet keySet; //our key store which should contain all JsonWebKey objects of the shared map  
	
	@Autowired
    public SPAKeyStoreRepository() throws Exception {
        String xmlFileName = System.getProperty("hazelcast.config");
        logger.info("Loading hazelcast configuration from :" + xmlFileName);
        Config hazelcastConfig = new Config();

        if (xmlFileName != null && xmlFileName.length() > 10) {
            try {
                hazelcastConfig = new XmlConfigBuilder(xmlFileName).build();
                logger.info("Loading hazelcast configuration from :" + xmlFileName);
            } catch (FileNotFoundException notFound) {
                logger.error("Error - not able to load hazelcast.xml configuration.  Using embedded configuration as fallback");
            }
        } else {
			logger.warn("Unable to load external hazelcast.xml configuration.  Using configuration from classpath as fallback");
			InputStream configFromClassPath = SPAKeyStoreRepository.class.getClassLoader().getResourceAsStream("hazelcast.xml");
			hazelcastConfig = new XmlConfigBuilder(configFromClassPath).build();
		}

        hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");
        //hazelcastConfig.getGroupConfig().setName("OID_HAZELCAST");
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        String gridPrefix = "SPAPROXY";
        map = hazelcastInstance.getMap(gridPrefix + "_keystoreMap");
        logger.info("Connecting to map {}", gridPrefix + "_keystoreMap");
        
        loadkeystoreFromLocalDisk();
    }
	
	private void loadkeystoreFromLocalDisk() throws Exception {
		
		if(FileUtils.localFileExist(keystoreSource)) {
			keySet = RsaJwkHelper.loadJWKS(keystoreSource);
			for(JsonWebKey key:keySet.getJsonWebKeys()) {

				if (StringUtils.isBlank(key.getAlgorithm())) {
					logger.warn("JSON web key {} has no algorithm defined", key);
				}
				if (StringUtils.isBlank(key.getKeyId())) {
					logger.warn("JSON web key {} has no key id defined", key);
					continue;
				}
				if (key.getKey() == null) {
					logger.warn("JSON web key {} has no key", key);
					continue;
				} else {
					map.put(key.getKeyId(), key);
				}

			}
		} else {
			RsaJsonWebKey key = RsaJwkHelper.produce();
			keySet = RsaJwkHelper.addToKeyStore(keystoreSource, key);
			//add to the map
			map.put(key.getKeyId(), key);
		}
	}
	
	private void syncMap() throws Exception {
		for(JsonWebKey key:new ArrayList<>(map.values())) {
			if(RsaJwkHelper.getKeyFromKeyStore(keySet, key.getKeyId())==null) {
				//the key belongs to another hazelcast instance member
				//we should add this key to our key store
				keySet.addJsonWebKey(key);
				
			}
		}
		//save to local disk
		RsaJwkHelper.saveKeystoretoFile(keystoreSource, keySet);
	}
	
	public RsaJsonWebKey getARandomKey() throws Exception {
		syncMap();
		return (RsaJsonWebKey) RsaJwkHelper.getKeyFromKeyStore(keySet, -1);
	}
	
	JsonWebKeySet getKeystore() throws Exception {
		syncMap();
		return keySet;
	}

	public String getUserTokenIdFromJWT(String jwt) {
		try {
			JwtClaims claims = null; 
			for(JsonWebKey jwk : getKeystore().getJsonWebKeys()) {
				claims = AdvancedJWTokenUtil.parseJWT(jwt, jwk.getKey());
				if(claims!=null) {
					break;
				}
			}

			//get usertokenid from the claims
			return claims.getJwtId();
		} catch(Exception ex) {
			//should not happen
			ex.printStackTrace();
			logger.error("Failed to retrieve the keystore before parsing JWT. Exception message = " + ex.getMessage());
		}
		return null;
	}
}
