package net.whydah.service.spasession;

import net.whydah.sso.application.types.Application;
import org.glassfish.jersey.uri.UriComponent;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 *
 */
public class ResponseUtilTest {

    @Test
    public void testSsoLoginRedirectUrl() {
        String ssoLoginUrl = "http://localhost:19997/oidsso/";
        String spaProxyUrl = "http://localhost:9898/spasession";
        Application application = new Application("testId", "my-spa-application");
        Map<String, String[]> queryParams = new HashMap<>();
        Response response = ResponseUtil.ssoLoginRedirectUrl(ssoLoginUrl, spaProxyUrl, application, queryParams);

        assertEquals(response.getStatus(), Response.Status.FOUND.getStatusCode());
        String expectedLocation = "http://localhost:19997/oidsso/login?redirectURI=http://localhost:9898/spasession/load/my-spa-application";
        String location = UriComponent.decode(response.getHeaderString("Location"), UriComponent.Type.QUERY_PARAM);
        assertEquals(location, expectedLocation);
    }


    @Test
    public void testSsoLoginRedirectUrl_withQueryParams() {
        String ssoLoginUrl = "http://localhost:19997/oidsso/";
        String spaProxyUrl = "http://localhost:9898/spasession";
        Application application = new Application("testId", "my-spa-application");
        Map<String, String[]> queryParams = Collections.singletonMap("UserCheckout", new String[] { "true" });
        Response response = ResponseUtil.ssoLoginRedirectUrl(ssoLoginUrl, spaProxyUrl, application, queryParams);

        assertEquals(response.getStatus(), Response.Status.FOUND.getStatusCode());
        String expectedLocation = "http://localhost:19997/oidsso/login?redirectURI=http://localhost:9898/spasession/load/my-spa-application&UserCheckout=true";
        String location = UriComponent.decode(response.getHeaderString("Location"), UriComponent.Type.QUERY_PARAM);
        assertEquals(location, expectedLocation);
    }


    @Test
    public void ssoLoginRedirectUrl_nullInput_InternalServerError() {
        String ssoLoginUrl = null;
        String spaProxyUrl = "http://localhost:9898/spasession";
        Application application = new Application("testId", "my-spa-application");
        Map<String, String[]> queryParams = new HashMap<>();
        //noinspection ConstantConditions
        Response response = ResponseUtil.ssoLoginRedirectUrl(ssoLoginUrl, spaProxyUrl, application, queryParams);

        assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}