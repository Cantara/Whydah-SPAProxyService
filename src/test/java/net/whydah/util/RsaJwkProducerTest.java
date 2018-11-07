package net.whydah.util;

import static org.testng.Assert.assertTrue;

import java.io.File;

import org.jose4j.jwk.JsonWebKey;
import org.testng.annotations.Test;

public class RsaJwkProducerTest {
	
	 @Test(enabled = true)
	 public void testProducingAndLoadingJwks() throws Exception {
		 //set where to store the key store
		 RsaJwkHelper.keyStoreSource = System.getProperty("java.io.tmpdir") + File.separator + "somekeystore.jwks"; 
		 System.out.println("Located source " + RsaJwkHelper.keyStoreSource);		 
		 File file = new File(RsaJwkHelper.keyStoreSource);
	     file.delete();
	     
	    
	     //produce a new one and save to the key store
	     //do not set param2 = true as it will save to the class path, we only want to test the external source 'somekeystore.jwks'
	     RsaJwkHelper.saveToKeyStore(RsaJwkHelper.produce(), false);
	     assertTrue(file.exists());
	     assertTrue(RsaJwkHelper.loadJWKS().getJsonWebKeys().size()==1);
	     System.out.println("JWKS: " + RsaJwkHelper.loadJWKS().toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
	     
	     //produce a new one and append this to the existing key store
	     RsaJwkHelper.saveToKeyStore(RsaJwkHelper.produce(), true);
	     assertTrue(file.exists());
	     assertTrue(RsaJwkHelper.loadJWKS().getJsonWebKeys().size()==2);
	     System.out.println("JWKS: " + RsaJwkHelper.loadJWKS().toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
	     
	     //produce a new one and override the key store
	     RsaJwkHelper.saveToKeyStore(RsaJwkHelper.produce(), false);
	     assertTrue(file.exists());
	     assertTrue(RsaJwkHelper.loadJWKS().getJsonWebKeys().size()==1);
	     
	     System.out.println("JWKS: " + RsaJwkHelper.loadJWKS().toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
	     
	     //clean up
	     file.delete();
	 }
	
}
