package net.whydah.util;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.lang.JoseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RsaJwkProducer {
    private static final Logger log = LoggerFactory.getLogger(RsaJwkProducer.class);

    private RsaJwkProducer() {
    }

    private static RsaJsonWebKey theOne;

    public static RsaJsonWebKey produce() {
        if (theOne == null) {
            try {
                theOne = RsaJwkGenerator.generateJwk(2048);
            } catch (JoseException ex) {
                log.error("Failed trying to generate Jwk", ex);
            }
        }

        log.info("RSA Key setup... " + theOne.hashCode());
        return theOne;
    }
}
