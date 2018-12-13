package net.whydah.service.auth.ssologin;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;
import java.util.UUID;

class SSOLoginSession implements DataSerializable {
    private UUID ssoLoginUUID;
    private String status;
    private String applicationName;

    private SSOLoginSession() {
    }

    SSOLoginSession(UUID ssoLoginUUID, String status, String applicationName) {
        this.ssoLoginUUID = ssoLoginUUID;
        this.status = status;
        this.applicationName = applicationName;
    }

    UUID getSsoLoginUUID() {
        return ssoLoginUUID;
    }

    String getStatus() {
        return status;
    }

    void setStatus(final String status) {
        this.status = status;
    }

    SSOLoginSession withStatus(final String status) {
        this.status = status;
        return this;
    }

    String getApplicationName() {
        return applicationName;
    }

    @Override
    public void writeData(final ObjectDataOutput out) throws IOException {
        out.writeUTF(ssoLoginUUID.toString());
        out.writeUTF(status);
        out.writeUTF(applicationName);
    }

    @Override
    public void readData(final ObjectDataInput in) throws IOException {
        ssoLoginUUID = UUID.fromString(in.readUTF());
        status = in.readUTF();
        applicationName = in.readUTF();
    }
}
