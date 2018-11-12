package net.whydah.service.proxy;

import net.whydah.util.StringXORer;

import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2018-11-12
 */
public class SPASessionSecret {
    private final String secretPart1;
    private final String secretPart2;
    private final String secret;

    SPASessionSecret() {
        this.secretPart1 = UUID.randomUUID().toString();
        this.secretPart2 = UUID.randomUUID().toString();
        this.secret = StringXORer.encode(secretPart1, secretPart2);
    }

    String getSecretPart1() {
        return secretPart1;
    }
    String getSecretPart2() {
        return secretPart2;
    }
    String getSecret() {
        return secret;
    }

    String getSimpleSecret(String applicationId) {
        return StringXORer.encode(secretPart1, applicationId);
    }

    @Override
    public String toString() {
        return "SPASessionSecret{" +
                "secretPart1='" + secretPart1 + '\'' +
                ", secretPart2='" + secretPart2 + '\'' +
                ", secret='" + secret + '\'' +
                '}';
    }
}
