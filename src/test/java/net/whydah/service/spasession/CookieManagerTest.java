package net.whydah.service.spasession;

import org.testng.annotations.Test;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2018-11-12
 */
public class CookieManagerTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void testGetUserTokenIdFromCookieNPEWithoutServletRequest() {
        CookieManager.getUserTokenIdFromCookie(null);
    }

    @Test
    public void testGetUserTokenIdFromCookie() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        assertNull(CookieManager.getUserTokenIdFromCookie(mockRequest));

        when(mockRequest.getCookies()).thenReturn(new Cookie[] {new Cookie("name1", "value1")});
        assertNull(CookieManager.getUserTokenIdFromCookie(mockRequest));


        String cookieValue = "userTokenId";
        Cookie[] cookies = new Cookie[] {
                new Cookie("name1", "value1"),
                new Cookie(CookieManager.userTokenReferenceName, cookieValue)
        };
        when(mockRequest.getCookies()).thenReturn(cookies);
        assertEquals(CookieManager.getUserTokenIdFromCookie(mockRequest), cookieValue);
    }
}
