package net.whydah.service.httpproxy;

import org.testng.annotations.Test;

import java.util.HashMap;

import static org.testng.Assert.*;

public class TemplateUtilTest {

    @Test
    public void verify_getCloneWithReplacements_tokenservice() throws CloneNotSupportedException {
        ProxySpecification proxySpecification = new ProxySpecification(
                "#securitytokenservice#applicationTokenId/#userTokenId",
                "application/json",
                "",
                "false",
                "true",
                "5000",
                "",
                new HashMap<>(),
                new HashMap<>()
        );
        String tokenservice = "https://sts.example.url.com/tokenservice/";
        String applicationTokenId = "applicationTokenIdHere";
        String userTokenId = "userTokenIdHere";

        ProxySpecification cloneWithReplacements = TemplateUtil.getCloneWithReplacements(proxySpecification, applicationTokenId, userTokenId, "", tokenservice);
        String expectedCommandUrl = "https://sts.example.url.com/tokenservice/applicationTokenIdHere/userTokenIdHere";
        assertEquals(cloneWithReplacements.getCommand_url(), expectedCommandUrl);
    }

    @Test
    public void verify_getCloneWithReplacements_logonservice() throws CloneNotSupportedException {
        ProxySpecification proxySpecification = new ProxySpecification(
                "#logonservice#applicationTokenId/#userTokenId",
                "application/json",
                "",
                "false",
                "true",
                "5000",
                "",
                new HashMap<>(),
                new HashMap<>()
        );
        String logonservice = "https://logon.example.url.com/sso/";
        String applicationTokenId = "applicationTokenIdHere";
        String userTokenId = "userTokenIdHere";

        ProxySpecification cloneWithReplacements = TemplateUtil.getCloneWithReplacements(proxySpecification, applicationTokenId, userTokenId, logonservice, "");
        String expectedCommandUrl = "https://logon.example.url.com/sso/applicationTokenIdHere/userTokenIdHere";
        assertEquals(cloneWithReplacements.getCommand_url(), expectedCommandUrl);
    }
}