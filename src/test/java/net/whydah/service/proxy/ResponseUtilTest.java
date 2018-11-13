package net.whydah.service.proxy;

import net.whydah.sso.application.types.Application;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.testng.Assert.*;

/**
 *
 */
public class ResponseUtilTest {

    @Test
    public void testSsoLoginRedirectUrl() {
        String ssoLoginUrl = "http://localhost:19997/oidsso/";
        String spaProxyUrl = "http://localhost:9898/proxy";
        Application application = new Application("testId", "my-spa-application");
        Response response = ResponseUtil.ssoLoginRedirectUrl(ssoLoginUrl, spaProxyUrl, application);

        assertEquals(response.getStatus(), Response.Status.FOUND.getStatusCode());
        String expectedLocation = "http://localhost:19997/oidsso/login?redirectURI=http://localhost:9898/proxy/load/my-spa-application";
        assertEquals(response.getHeaderString("Location"), expectedLocation);
    }

    @Test
    public void ssoLoginRedirectUrl_nullInput_InternalServerError() {
        String ssoLoginUrl = null;
        String spaProxyUrl = "http://localhost:9898/proxy";
        Application application = new Application("testId", "my-spa-application");
        //noinspection ConstantConditions
        Response response = ResponseUtil.ssoLoginRedirectUrl(ssoLoginUrl, spaProxyUrl, application);

        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}