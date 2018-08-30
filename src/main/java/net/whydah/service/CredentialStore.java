package net.whydah.service;

import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationACL;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.basehelpers.Validator;
import net.whydah.sso.commands.adminapi.application.CommandGetApplication;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.session.WhydahUserSession;
import net.whydah.sso.user.types.UserCredential;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.inject.Singleton;
import java.net.URI;
import java.util.List;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Singleton
@Repository
public class CredentialStore {
    private final String stsUri;
    private final String uasUri;
    private final ApplicationCredential myApplicationCredential;
    private static WhydahApplicationSession was = null;
    private final UserCredential adminUserCredential;
    private static WhydahUserSession adminUserSession = null;
    private static final Logger log = LoggerFactory.getLogger(CredentialStore.class);
    public static final String FALLBACK_URL =  net.whydah.util.Configuration.getString("fallbackurl");

    @Autowired
    @Configure
    public CredentialStore(@Configuration("securitytokenservice") String stsUri,
                           @Configuration("useradminservice") String uasUri,
                           @Configuration("applicationid") String applicationid,
                           @Configuration("applicationname") String applicationname,
                           @Configuration("applicationsecret") String applicationsecret,
                           @Configuration("adminuserid") String adminuserid,
                           @Configuration("adminusersecret") String adminusersecret) {
        this.stsUri = stsUri;
        this.uasUri = uasUri;
        this.myApplicationCredential = new ApplicationCredential(applicationid, applicationname, applicationsecret);
        this.adminUserCredential = new UserCredential(adminuserid,adminusersecret);

    }


    public String getUserAdminServiceTokenId() {
        if (was == null) {
            was = WhydahApplicationSession.getInstance(stsUri, uasUri, myApplicationCredential);
        }
        if (hasWhydahConnection()){
            return was.getActiveApplicationTokenId();
        }
        return null;
    }

    public boolean hasWhydahConnection() {
        if (was == null) {
            return false;
        }
        return getWas().checkActiveSession();
    }


    public String hasApplicationToken() {
        try {
            if (hasWhydahConnection()) {
                return Boolean.toString(getWas().getActiveApplicationTokenId() != null);
            }
        } catch (Exception e) {
        }
        return Boolean.toString(false);
    }

    public String hasValidApplicationToken() {
        try {
            if (hasWhydahConnection()) {
                return Boolean.toString(getWas().checkActiveSession());
            }
        } catch (Exception e) {
        }
        return Boolean.toString(false);
    }

    public String hasApplicationsMetadata() {
        try {
            if (hasWhydahConnection()) {
                was.updateApplinks(true);
                return Boolean.toString(getWas().getApplicationList().size() > 2);
            }
        } catch (Exception e) {
        }
        return Boolean.toString(false);
    }


    public WhydahApplicationSession getWas() {
        if (was == null) {
            was = WhydahApplicationSession.getInstance(stsUri, uasUri, myApplicationCredential);
            if (hasWhydahConnection()) {
                was.updateApplinks(true);
            }
        }
        return was;
    }

    public String getAdminUserTokenId(){
        if (adminUserSession == null) {
            adminUserSession = getAdminUserSession();
        }
        return adminUserSession.getActiveUserTokenId();
    }
    public WhydahUserSession getAdminUserSession() {
        if (adminUserSession == null) {
            adminUserSession =  new WhydahUserSession(getWas(),adminUserCredential);
        }
        return adminUserSession;
    }

    public Application findApplication(String appName){

        List<Application> applicationList = getWas().getApplicationList();
        log.debug("Found {} applications",applicationList.size());
        Application found = null;
        for (Application application:applicationList){
            log.info("Parsing application: {}",application.getName());

            if (application.getName().equalsIgnoreCase(appName)){
                found = application;
                break;
            }
            if (application.getId().equalsIgnoreCase(appName)){
                found = application;
                break;
            }
        }

        if(found!=null){
            //HUY: we have to use admin function to get the full specification of this app
            //Problem is the app secret is obfuscated in application list
            CommandGetApplication app = new CommandGetApplication(URI.create(getWas().getUAS()), getWas().getActiveApplicationTokenId(), getAdminUserTokenId(), found.getId());
            String result = app.execute();
            if(result!=null){
                found = ApplicationMapper.fromJson(result);
            }
        }
        return found;
    }

    public String findRedirectUrl(Application application) {
        String redirectUrl = null;

        if (application != null && application.getAcl() != null) {
            List<ApplicationACL> acls = application.getAcl();
            for (ApplicationACL acl : acls) {
                if (acl.getAccessRights() != null && acl.getAccessRights().contains(ApplicationACL.OAUTH2_REDIRECT)) {
                    redirectUrl = acl.getApplicationACLPath();
                    log.trace("Found redirectpath {} for application {}", redirectUrl, application.getId());
                }
            }
        }

        if (redirectUrl==null && application!=null && application.getApplicationUrl()!=null && Validator.isValidURL(application.getApplicationUrl())){
            redirectUrl=application.getApplicationUrl();
        }
        if (redirectUrl==null){
            redirectUrl= FALLBACK_URL;
        }

        return redirectUrl;
    }

}
