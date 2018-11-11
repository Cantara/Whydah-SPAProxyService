package net.whydah.util;

import org.jose4j.jwk.JsonWebKey;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RsaJwkProducerTest {
	
	 @Test()
	 public void testProducingAndLoadingJwks() throws Exception {
		 //set where to store the key store
		 RsaJwkHelper.keyStoreSource = System.getProperty("java.io.tmpdir") + File.separator + "somekeystore.jwks"; 
		 System.out.println("Located source " + RsaJwkHelper.keyStoreSource);		 
		 File file = new File(RsaJwkHelper.keyStoreSource);
	     file.delete();
	     
	    
	     //produce a new one and save to the key store
	     RsaJwkHelper.saveToKeyStore(RsaJwkHelper.produce(), false);
	     assertTrue(file.exists());
		 assertEquals(1, RsaJwkHelper.loadJWKS().getJsonWebKeys().size());
	     System.out.println("JWKS: " + RsaJwkHelper.loadJWKS().toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
	     
	     //produce a new one and append this to the existing key store
	     RsaJwkHelper.saveToKeyStore(RsaJwkHelper.produce(), true);
	     assertTrue(file.exists());
		 assertEquals(2, RsaJwkHelper.loadJWKS().getJsonWebKeys().size());
	     System.out.println("JWKS: " + RsaJwkHelper.loadJWKS().toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
	     
	     //produce a new one and override the key store
	     RsaJwkHelper.saveToKeyStore(RsaJwkHelper.produce(), false);
	     assertTrue(file.exists());
		 assertEquals(1, RsaJwkHelper.loadJWKS().getJsonWebKeys().size());
	     
	     System.out.println("JWKS: " + RsaJwkHelper.loadJWKS().toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
	     
	     //clean up
	     file.delete();
	 }
	 
	 //enable this to generate a new spa-keystore.jwks stored in the project directory
	 @Test(enabled = false)
	 public void testProducingJWKs() throws Exception {
		 RsaJwkHelper.saveToKeyStore(RsaJwkHelper.produce(), false);
	 }
	
}
