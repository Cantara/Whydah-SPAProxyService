package net.whydah.service.auth.ssologin;

import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * Used to verify serialisation of objects to the hazecast map
 */
public class SSOLoginRepositoryTest {

    @Test
    public void verifySerialization() {
        SSOLoginRepository ssoLoginRepository = new SSOLoginRepository(60, 60, 60);

        UUID uuid = UUID.randomUUID();
        SSOLoginSession testApp = new SSOLoginSession(uuid, SessionStatus.INITIALIZED,
                "testApp", true);


        ssoLoginRepository.put(uuid, testApp);


        SSOLoginSession retrievedSession = ssoLoginRepository.get(uuid);

        assertEquals(retrievedSession.getSsoLoginUUID(), testApp.getSsoLoginUUID());
        assertEquals(retrievedSession.getStatus(), testApp.getStatus());
        assertEquals(retrievedSession.getApplicationName(), testApp.getApplicationName());
        assertEquals(retrievedSession.getUserTicket(), testApp.getUserTicket());
        ZonedDateTime initialized = retrievedSession.getInitializedTimestamp();
        assertNotNull(initialized);


        retrievedSession
                .withStatus(SessionStatus.COMPLETE)
                .withUserTicket("testTicket");


        ssoLoginRepository.put(uuid, retrievedSession);

        SSOLoginSession modifiedSession = ssoLoginRepository.get(uuid);

        assertEquals(modifiedSession.getUserTicket(), "testTicket");
        assertEquals(modifiedSession.getStatus(), SessionStatus.COMPLETE);
        assertEquals(modifiedSession.getInitializedTimestamp(), initialized);


        ssoLoginRepository.remove(uuid);

        SSOLoginSession deletedSession = ssoLoginRepository.get(uuid);

        assertNull(deletedSession);
    }

    @Test
    public void verifyAutomatedDeletion() throws InterruptedException {
        SSOLoginRepository ssoLoginRepository = new SSOLoginRepository(2, 1, 1);

        UUID uuid = UUID.randomUUID();
        SSOLoginSession testApp = new SSOLoginSession(uuid, SessionStatus.INITIALIZED,
                "testApp", true);
        ssoLoginRepository.put(uuid, testApp);


        SSOLoginSession retrievedSession = ssoLoginRepository.get(uuid);

        assertEquals(retrievedSession.getSsoLoginUUID(), testApp.getSsoLoginUUID());
        assertEquals(retrievedSession.getStatus(), testApp.getStatus());
        assertEquals(retrievedSession.getApplicationName(), testApp.getApplicationName());
        assertEquals(retrievedSession.getUserTicket(), testApp.getUserTicket());
        ZonedDateTime initialized = retrievedSession.getInitializedTimestamp();
        assertNotNull(initialized);

        //Thread should have automaticall deleted the old session after maxAgeSeconds

        TimeUnit.SECONDS.sleep(2);
        SSOLoginSession afterCleanUp = ssoLoginRepository.get(uuid);
        assertNull(afterCleanUp);
    }

}