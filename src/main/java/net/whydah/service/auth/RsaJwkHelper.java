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
	//source to store the key store (class path /disk)
	public static String keyStoreSource = net.whydah.util.Configuration.getString("keystoresource");
	
	//load the key store from source and get a RSA key in the first index
	//we should use loadARandomJWK() instead although we only have one key for now
	public static RsaJsonWebKey loadTheFristJWK() {

		try {
			InputStream is = openInputStream();
			String json = FileUtils.read(is);
			RsaJsonWebKey key= (RsaJsonWebKey) getKeyFromKeyStore(json, 0);
			return key;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Unexpected exception when reading keystore");
			return null;
		}
	}

	//load a random RSA key, encourage to use this one
	public static RsaJsonWebKey loadARandomJWK() {

		try {
			InputStream is = openInputStream();
			String json = FileUtils.read(is);
			RsaJsonWebKey key= (RsaJsonWebKey) getKeyFromKeyStore(json, -1);
			return key;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Unexpected exception when reading keystore");
			return null;
		}
	}

	//get the key in the key store at a specific index
	public static JsonWebKey getKeyFromKeyStore(final String jsonKeyStore, int index) throws Exception {
		JsonWebKeySet jwks = new JsonWebKeySet(jsonKeyStore);
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
	
	//get the key in the key store at the first index
	public static JsonWebKey getKeyFromKeyStore(final String jsonKeyStore) throws Exception {
		return getKeyFromKeyStore(jsonKeyStore, 0);
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

	//open the source input stream 
	static InputStream openInputStream() {
		InputStream is;
		if (FileUtils.localFileExist(keyStoreSource)) {
			logger.info("Importing keystore from local file {}", keyStoreSource);
			is = FileUtils.openLocalFile(keyStoreSource);
		} else {
			logger.info("Importing keystore from classpath {}", keyStoreSource);
			is = FileUtils.openFileOnClasspath(keyStoreSource);
		}
		return is;
	}
	
	//get the key store (JWKS) from source
	public static JsonWebKeySet loadJWKS() throws Exception {
		InputStream is = openInputStream();
		String json = FileUtils.read(is);
		JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(json);
		return jsonWebKeySet;
	}
	
	//save a new key to key store
	public static void saveToKeyStore(JsonWebKey jwk, boolean append) throws Exception {
		JsonWebKeySet jsonWebKeySet;
		if(append) {
			jsonWebKeySet = loadJWKS();
			jsonWebKeySet.addJsonWebKey(jwk);
		} else {
			jsonWebKeySet = new JsonWebKeySet(jwk);
		}				
		String data = jsonWebKeySet.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE);
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(keyStoreSource), "utf-8"))) {
			writer.write(data);
		}
	}
	
	//produce a new Rsa key
	public static RsaJsonWebKey produce() {
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
