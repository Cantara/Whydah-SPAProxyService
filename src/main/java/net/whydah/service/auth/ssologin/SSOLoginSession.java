package net.whydah.service.auth.ssologin;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

class SSOLoginSession implements DataSerializable {
    private UUID ssoLoginUUID;
    private SessionStatus status;
    private String applicationName;
    private String spaSessionSecretHash;
    private String userTicket;
    private ZonedDateTime initializedTimestamp;

    private SSOLoginSession() {
    }

    /**
     * Use this constructor when creating new SSOLoginSessions initiated without a SPA Session
     */
    SSOLoginSession(UUID ssoLoginUUID, SessionStatus status, String applicationName) {
        this(ssoLoginUUID, status, applicationName, null);
    }

    /**
     * Use this constructor when creating new SSOLoginSessions initiated with a SPA Session.
     * The SSOLoginSession uses a hashed version of the spaSessionSecret for comparison purposes.
     * This avoids storing the sessionSecret multiple times.
     * See {@link net.whydah.service.auth.ssologin.SSOLoginUtil#sha256Hash(String)}
     */
    SSOLoginSession(UUID ssoLoginUUID, SessionStatus status, String applicationName, String spaSessionSecretHash) {
        this.ssoLoginUUID = ssoLoginUUID;
        this.status = status;
        this.applicationName = applicationName;
        this.spaSessionSecretHash = spaSessionSecretHash;
        this.initializedTimestamp = ZonedDateTime.now();
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

    String getSpaSessionSecretHash() {
        return spaSessionSecretHash;
    }

    SSOLoginSession withSpaSessionSecretHash(String spaSessionSecretHash) {
        this.spaSessionSecretHash = spaSessionSecretHash;
        return this;
    }

    boolean hasSpaSessionSecretHash() {
        return spaSessionSecretHash != null && !spaSessionSecretHash.isEmpty();
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

    ZonedDateTime getInitializedTimestamp() {
        return initializedTimestamp;
    }

    @Override
    public void writeData(final ObjectDataOutput out) throws IOException {
        out.writeUTF(ssoLoginUUID.toString());
        out.writeUTF(status.name());
        out.writeUTF(applicationName);
        out.writeUTF(userTicket);
        out.writeUTF(spaSessionSecretHash);
        out.writeUTF(initializedTimestamp.toString());
    }

    @Override
    public void readData(final ObjectDataInput in) throws IOException {
        ssoLoginUUID = UUID.fromString(in.readUTF());
        status = SessionStatus.valueOf(in.readUTF());
        applicationName = in.readUTF();
        userTicket = in.readUTF();
        spaSessionSecretHash = in.readUTF();
        initializedTimestamp = ZonedDateTime.parse(in.readUTF());
    }
}
