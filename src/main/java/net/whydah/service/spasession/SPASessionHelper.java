package net.whydah.service.spasession;

import net.whydah.service.CredentialStore;
import net.whydah.service.SPAApplicationRepository;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.userauth.CommandCreateTicketForUserTokenID;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUserticket;
import net.whydah.sso.user.helpers.UserTokenXpathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.UUID;

/**
 * Use a helper object to avoid duplication and hold logic shared by multiple endpoints in {@link SPASessionResource}.
 * Main responsibilities are ticket renewal and adding a new references to the application session map.
 *
 * @author Totto
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
public class SPASessionHelper {
    private static final Logger log = LoggerFactory.getLogger(SPASessionHelper.class);

    private final CredentialStore credentialStore;
    private final SPAApplicationRepository spaApplicationRepository;

    public SPASessionHelper(CredentialStore credentialStore, SPAApplicationRepository spaApplicationRepository) {
        this.credentialStore = credentialStore;
        this.spaApplicationRepository = spaApplicationRepository;
    }


    String renewTicketWithUserTicket(String userticket) {
        log.debug("User ticket from request param is {}", userticket);

        if (userticket == null) {
            return null;
        }
        String userTokenXml = getUserTokenXml(userticket);
        if (userTokenXml == null) {
            log.debug("User token xml is null");
            return null;
        }

        String userTokenId = UserTokenXpathHelper.getUserTokenId(userTokenXml);
        return renewTicket(userTokenId);
    }
    String renewTicket(String userTokenId) {
        log.debug("Renewing userticket using userTokenId={}", userTokenId);

        if (userTokenId == null) {
            return null;
        }

        String newTicket = UUID.randomUUID().toString();
        if (!generateAUserTicket(userTokenId, newTicket)) {
            log.debug("Should not generate a new ticket. Reverting to null");
            newTicket = null;
        } else {
            log.debug("Generated a new ticket");
        }
        return newTicket;
    }

    private String getUserTokenXml(String userticket) {
        return new CommandGetUsertokenByUserticket(URI.create(credentialStore.getWas().getSTS()),
                credentialStore.getWas().getActiveApplicationTokenId(),
                credentialStore.getWas().getActiveApplicationTokenXML(),
                userticket
        ).execute();
    }

    private boolean generateAUserTicket(String userTokenId, String ticket) {
        CommandCreateTicketForUserTokenID cmt = new CommandCreateTicketForUserTokenID(URI.create(credentialStore.getWas().getSTS()),
                credentialStore.getWas().getActiveApplicationTokenId(), credentialStore.getWas().getActiveApplicationTokenXML(),
                ticket, userTokenId);

        boolean result = cmt.execute();

        if (result) {
            log.debug("create a ticket {} for usertoken {}", ticket, userTokenId);
        } else {
            log.warn("failed to create a ticket {} for usertoken {}", ticket, userTokenId);
        }
        return result;
    }




    public SPASessionSecret addReferenceToApplicationSession(Application application) {
        SPASessionSecret spaSessionSecret = new SPASessionSecret();
        log.debug("establish new SPA secret and store it in secret-applicationsession map." + spaSessionSecret);
        spaApplicationRepository.add(spaSessionSecret.getSecret(), getOrCreateSessionForApplication(application));

        return spaSessionSecret;
    }

    private ApplicationToken getOrCreateSessionForApplication(Application application) {
        ApplicationToken applicationToken = getApplicationTokenFromSessions(application);

        if (applicationToken == null) {
            return createApplicationToken(application);
        } else {
            return applicationToken;
        }
    }
    private ApplicationToken getApplicationTokenFromSessions(Application application) {
        for (ApplicationToken applicationToken : spaApplicationRepository.allSessions()) {
            if (applicationToken.getApplicationID().equalsIgnoreCase(application.getId())) {
                return applicationToken;
            }
        }
        return null;
    }
    private ApplicationToken createApplicationToken(Application application) {
        ApplicationCredential appCredential = new ApplicationCredential(application.getId(), application.getName(),
                application.getSecurity().getSecret());
        String appTokenXml = new CommandLogonApplication(URI.create(credentialStore.getWas().getSTS()), appCredential).execute();

        return ApplicationTokenMapper.fromXml(appTokenXml);
    }
}
