package net.whydah.service.auth;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.testng.annotations.Test;

import net.whydah.service.SPAKeyStoreRepository;

import java.io.File;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RsaJwkProducerTest {
	
	 @Test()
	 public void testProducingAndLoadingJwks() throws Exception {
		 

		 //set where to store the key store
		 String keystoreSource = System.getProperty("java.io.tmpdir") + File.separator + "somekeystore.jwks"; 
		 System.out.println("Located source " + keystoreSource);		 
		 File file = new File(keystoreSource);
	     file.delete();
	     
	     
	 
	     //produce a new one and save to the key store
	     RsaJwkHelper.addToKeyStore(keystoreSource, RsaJwkHelper.produce());
	     assertTrue(file.exists());
	     
	     JsonWebKeySet keyset = RsaJwkHelper.loadJWKS(keystoreSource);
		 assertEquals(keyset.getJsonWebKeys().size(), 1);
	     System.out.println("JWKS: " + keyset.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
	     
	     //produce a new one and append this to the existing key store
	     RsaJwkHelper.addToKeyStore(keystoreSource, RsaJwkHelper.produce());
	     assertTrue(file.exists());
	     keyset = RsaJwkHelper.loadJWKS(keystoreSource);
		 assertEquals(keyset.getJsonWebKeys().size(), 2);
	     System.out.println("JWKS: " + keyset.toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
	     
	     //clean up
	     file.delete();
	    
	 }
	
}
