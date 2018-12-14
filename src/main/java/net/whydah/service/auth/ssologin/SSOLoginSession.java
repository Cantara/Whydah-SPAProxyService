package net.whydah.service.auth.ssologin;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;
import java.util.UUID;

class SSOLoginSession implements DataSerializable {
    private UUID ssoLoginUUID;
    private SessionStatus status;
    private String applicationName;
    private String spaSessionSecret;
    private String userTicket;

    private SSOLoginSession() {
    }

    SSOLoginSession(UUID ssoLoginUUID, SessionStatus status, String applicationName) {
        this.ssoLoginUUID = ssoLoginUUID;
        this.status = status;
        this.applicationName = applicationName;
    }

    SSOLoginSession(UUID ssoLoginUUID, SessionStatus status, String applicationName, String spaSessionSecret) {
        this.ssoLoginUUID = ssoLoginUUID;
        this.status = status;
        this.applicationName = applicationName;
        this.spaSessionSecret = spaSessionSecret;
    }

    UUID getSsoLoginUUID() {
        return ssoLoginUUID;
    }

    SessionStatus getStatus() {
        return status;
    }

    void setStatus(final SessionStatus status) {
        this.status = status;
    }

    SSOLoginSession withStatus(final SessionStatus status) {
        this.status = status;
        return this;
    }

    String getApplicationName() {
        return applicationName;
    }

    String getSpaSessionSecret() {
        return spaSessionSecret;
    }

    void setSpaSessionSecret(final String spaSessionSecret) {
        this.spaSessionSecret = spaSessionSecret;
    }

    SSOLoginSession withSpaSessionSecret(final String spaSessionSecret) {
        this.spaSessionSecret = spaSessionSecret;
        return this;
    }

    String getUserTicket() {
        return userTicket;
    }

    void setUserTicket(final String userTicket) {
        this.userTicket = userTicket;
    }

    SSOLoginSession withUserTicket(final String userTicket) {
        this.userTicket = userTicket;
        return this;
    }

    @Override
    public void writeData(final ObjectDataOutput out) throws IOException {
        out.writeUTF(ssoLoginUUID.toString());
        out.writeUTF(status.name());
        out.writeUTF(applicationName);
        out.writeUTF(spaSessionSecret);
    }

    @Override
    public void readData(final ObjectDataInput in) throws IOException {
        ssoLoginUUID = UUID.fromString(in.readUTF());
        status = SessionStatus.valueOf(in.readUTF());
        applicationName = in.readUTF();
        spaSessionSecret = in.readUTF();
    }
}
