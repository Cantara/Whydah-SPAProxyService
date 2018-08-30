package net.whydah.util;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.lang.JoseException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RsaJwkProducer {

    private RsaJwkProducer() {
    }

    private static RsaJsonWebKey theOne;

    public static RsaJsonWebKey produce() {
        if (theOne == null) {
            try {
                theOne = RsaJwkGenerator.generateJwk(2048);
            } catch (JoseException ex) {
                Logger.getLogger(RsaJwkProducer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.out.println("RSA Key setup... " + theOne.hashCode());
        return theOne;
    }
}
