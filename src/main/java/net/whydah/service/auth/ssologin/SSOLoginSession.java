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
    private boolean hasSpaSessionSecret;
    private String userTicket;

    private SSOLoginSession() {
    }

    SSOLoginSession(UUID ssoLoginUUID, SessionStatus status, String applicationName, Boolean hasSpaSessionSecret) {
        this.ssoLoginUUID = ssoLoginUUID;
        this.status = status;
        this.applicationName = applicationName;
        this.hasSpaSessionSecret = hasSpaSessionSecret;
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

    boolean hasSpaSessionSecret() {
        return hasSpaSessionSecret;
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
        out.writeUTF(String.valueOf(hasSpaSessionSecret));
    }

    @Override
    public void readData(final ObjectDataInput in) throws IOException {
        ssoLoginUUID = UUID.fromString(in.readUTF());
        status = SessionStatus.valueOf(in.readUTF());
        applicationName = in.readUTF();
        hasSpaSessionSecret = Boolean.parseBoolean(in.readUTF());
    }
}
