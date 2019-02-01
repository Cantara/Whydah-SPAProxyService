package net.whydah.service.auth.ssologin;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * Verifies serialisation of objects to the hazelcast map and automated deletion from the map
 * Used to verify serialisation of objects to the hazecast map
 */
public class SSOLoginRepositoryTest {
    private SSOLoginRepository ssoLoginRepository;

    @BeforeClass
    public void setupRepository() {
        ssoLoginRepository = new SSOLoginRepository(2, 1, 1);

    }

    @Test
    public void verifySerialization() {
        UUID uuid = UUID.randomUUID();
        SSOLoginSession testApp = new SSOLoginSession(uuid, SessionStatus.INITIALIZED,
                "testApp", "secretHash");


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
        UUID uuid = UUID.randomUUID();
        SSOLoginSession testApp = new SSOLoginSession(uuid, SessionStatus.INITIALIZED,
                "testApp", "secretHash");
        ssoLoginRepository.put(uuid, testApp);


        SSOLoginSession retrievedSession = ssoLoginRepository.get(uuid);

        assertEquals(retrievedSession.getSsoLoginUUID(), testApp.getSsoLoginUUID());
        assertEquals(retrievedSession.getStatus(), testApp.getStatus());
        assertEquals(retrievedSession.getApplicationName(), testApp.getApplicationName());
        assertEquals(retrievedSession.getUserTicket(), testApp.getUserTicket());
        ZonedDateTime initialized = retrievedSession.getInitializedTimestamp();
        assertNotNull(initialized);

        //Thread should have automatically deleted the old session after maxAgeSeconds

        TimeUnit.SECONDS.sleep(3);
        SSOLoginSession afterCleanUp = ssoLoginRepository.get(uuid);
        assertNull(afterCleanUp);
    }

}