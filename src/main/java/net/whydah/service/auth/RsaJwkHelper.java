package net.whydah.service.auth;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.UUID;

public final class RsaJwkHelper {
	private static final Logger logger = LoggerFactory.getLogger(RsaJwkHelper.class);
	
	//get the key in the key store at a specific index
	private static JsonWebKey getKeyFromKeyStore(final String jsonKeyStore, int index) throws Exception {
		JsonWebKeySet jwks = new JsonWebKeySet(jsonKeyStore);
		return getKeyFromKeyStore(jwks, index);
	}
	
	//get the key in the key store at the first index
	public static JsonWebKey getKeyFromKeyStore(final String jsonKeyStore) throws Exception {
		return getKeyFromKeyStore(jsonKeyStore, 0);
	}
	
	static JsonWebKey getKeyFromKeyStore(JsonWebKeySet jwks, int index) throws Exception {
		if (jwks.getJsonWebKeys().isEmpty()) {
			logger.warn("No JSON web keys are available in the keystore");
			return null;
		}
		JsonWebKey key;
		if(index==-1) {
			 key = (JsonWebKey) jwks.getJsonWebKeys().get(RandomUtils.nextInt(jwks.getJsonWebKeys().size()));
		} else {
		     key = (JsonWebKey) jwks.getJsonWebKeys().get(index);
			if (StringUtils.isBlank(key.getAlgorithm())) {
				logger.warn("JSON web key {} has no algorithm defined", key);
			}
			if (StringUtils.isBlank(key.getKeyId())) {
				logger.warn("JSON web key {} has no key id defined", key);
			}

			if (key.getKey() == null) {
				logger.warn("JSON web key {} has no key", key);
				return null;
			}
		}
		
		return key;
	}
	
	//find a key with its id
	public static JsonWebKey getKeyFromKeyStore(final String jsonKeyStore, String keyId) throws Exception {
		JsonWebKeySet jwks = new JsonWebKeySet(jsonKeyStore);
		if (jwks.getJsonWebKeys().isEmpty()) {
			logger.warn("No JSON web keys are available in the keystore");
			return null;
		}
		for(JsonWebKey key:jwks.getJsonWebKeys()) {
			if(key.getKeyId().equals(keyId)) {
				if (StringUtils.isBlank(key.getAlgorithm())) {
					logger.warn("JSON web key {} has no algorithm defined", key);
				}
				if (StringUtils.isBlank(key.getKeyId())) {
					logger.warn("JSON web key {} has no key id defined", key);
				}
				if (key.getKey() == null) {
					logger.warn("JSON web key {} has no key", key);
					return null;
				} else {
					return key;
				}
			}
		}
		return null;
	}

	static JsonWebKey getKeyFromKeyStore(JsonWebKeySet jwks, String keyId) throws Exception {
		if (jwks.getJsonWebKeys().isEmpty()) {
			logger.warn("No JSON web keys are available in the keystore");
			return null;
		}
		for(JsonWebKey key:jwks.getJsonWebKeys()) {
			if(key.getKeyId().equals(keyId)) {
				if (StringUtils.isBlank(key.getAlgorithm())) {
					logger.warn("JSON web key {} has no algorithm defined", key);
				}
				if (StringUtils.isBlank(key.getKeyId())) {
					logger.warn("JSON web key {} has no key id defined", key);
				}
				if (key.getKey() == null) {
					logger.warn("JSON web key {} has no key", key);
					return null;
				} else {
					return key;
				}
			}
		}
		return null;
	}
	
	//open the source input stream 
	private static InputStream openInputStream(String keyStoreSource) {
		InputStream is = null;
		if (FileUtils.localFileExist(keyStoreSource)) {
			logger.info("Importing keystore from local file {}", keyStoreSource);
			is = FileUtils.openLocalFile(keyStoreSource);
		}
		return is;
	}
	
	//get the key store (JWKS) from source
	static JsonWebKeySet loadJWKS(String keystoreSource) throws Exception {
		InputStream is = openInputStream(keystoreSource);
		if(is!=null) {
			String json = FileUtils.read(is);
			is.close();
			JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(json);
			return jsonWebKeySet;
		} else {
			return null;
		}
	}
	
	//save a new key to key store
	static JsonWebKeySet addToKeyStore(String keystoreSource, JsonWebKey jwk) throws Exception {
		JsonWebKeySet jsonWebKeySet = loadJWKS(keystoreSource);
		if(jsonWebKeySet!=null) {
			jsonWebKeySet.addJsonWebKey(jwk);
		} else {
			jsonWebKeySet = new JsonWebKeySet(jwk);
		}				
		return saveKeystoretoFile(keystoreSource, jsonWebKeySet);
	}

	static JsonWebKeySet saveKeystoretoFile(String keystoreSource, JsonWebKeySet jsonWebKeySet)
			throws IOException, UnsupportedEncodingException, FileNotFoundException {
		String data = jsonWebKeySet.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE);
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(keystoreSource), "utf-8"))) {
			writer.write(data);
		}
		return jsonWebKeySet;
	}
	
	
	//produce a new Rsa key
	static RsaJsonWebKey produce() {
		try {
			RsaJsonWebKey theOne = RsaJwkGenerator.generateJwk(2048);
			theOne.setKeyId(UUID.randomUUID().toString());
			return theOne;
		} catch (Exception ex) {
			logger.error("Failed trying to generate Jwk", ex);
		}
		return null;
	}
}
