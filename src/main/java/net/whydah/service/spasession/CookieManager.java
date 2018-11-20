package net.whydah.service.spasession;

//import net.whydah.sso.user.mappers.UserTokenMapper;
//import net.whydah.sso.user.types.UserToken;

import net.whydah.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;

//import javax.servlet.http.HttpServletResponse;

final class CookieManager {
    //private static final String LOGOUT_COOKIE_VALUE = "logout";
    private static final Logger log = LoggerFactory.getLogger(CookieManager.class);
    //private static final int DEFAULT_COOKIE_MAX_AGE = 365 * 24 * 60 * 60;

    static String userTokenReferenceName = "whydahusertoken_sso";
    private static String cookieDomain = null;
    private static boolean isMyUriSecured = false;

    private CookieManager() {
    }

    static {
        try {
            cookieDomain = Configuration.getString("whydah.cookiedomain");
            String myAppUri = Configuration.getString("myuri");
            userTokenReferenceName = Configuration.getString("cookie_user_token_reference_name");
            //some overrides
            URL uri;

            if (myAppUri != null) {
                uri = new URL(myAppUri);
                isMyUriSecured = myAppUri.startsWith("https");

                if (cookieDomain == null || cookieDomain.isEmpty()) {
                    String domain = uri.getHost();
                    domain = domain.startsWith("www.") ? domain.substring(4) : domain;
                    cookieDomain = domain;
                }
            }
        } catch (IOException e) {
            log.warn("AppConfig.readProperties failed. cookieDomain was set to {}", cookieDomain, e);
        }
    }

    /*
    public static void addSecurityHTTPHeaders(HttpServletResponse response) {
        //TODO Vi trenger en plan her.
        //response.setHeader("X-Frame-Options", "sameorigin");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "master-only");
    }

    public static Integer calculateTokenRemainingLifetimeInSeconds(UserToken usertoken) {
        Long tokenLifespanSec = Long.parseLong(usertoken.getLifespan());
        Long tokenTimestampMsSinceEpoch = Long.parseLong(usertoken.getTimestamp());

        if (tokenLifespanSec == null || tokenTimestampMsSinceEpoch == null) {
            return null;
        }
        long endOfTokenLifeMs = tokenTimestampMsSinceEpoch + tokenLifespanSec;
        long remainingLifeMs = endOfTokenLifeMs - System.currentTimeMillis();

        return (int) (remainingLifeMs / 1000);
    }


    public static void createAndSetUserTokenCookie(UserToken ut, HttpServletRequest request,
                                                   HttpServletResponse response) {
        Integer tokenRemainingLifetimeSeconds = calculateTokenRemainingLifetimeInSeconds(ut);
        CookieManager.createAndSetUserTokenCookie(ut.getUserTokenId(), tokenRemainingLifetimeSeconds, request, response);
    }

    public static void createAndSetUserTokenCookie(String userTokenXml, HttpServletRequest request,
                                                   HttpServletResponse response) {
        UserToken ut = UserTokenMapper.fromUserTokenXml(userTokenXml);
        Integer tokenRemainingLifetimeSeconds = calculateTokenRemainingLifetimeInSeconds(ut);
        CookieManager.createAndSetUserTokenCookie(ut.getUserTokenId(), tokenRemainingLifetimeSeconds, request, response);
    }

    private static void createAndSetUserTokenCookie(String userTokenId, Integer tokenRemainingLifetimeSeconds,
                                                    HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(userTokenReferenceName, userTokenId);
        cookie.setValue(userTokenId);

        if (tokenRemainingLifetimeSeconds == null) {
            tokenRemainingLifetimeSeconds = DEFAULT_COOKIE_MAX_AGE;
        }
        addCookie(cookie.getValue(), tokenRemainingLifetimeSeconds, response);
    }


    public static void updateUserTokenCookie(String userTokenId, Integer tokenRemainingLifetimeSeconds,
                                             HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getUserTokenCookie(request);
        if (cookie == null) {
            cookie = new Cookie(userTokenReferenceName, userTokenId);
        }
        updateCookie(cookie, userTokenId, tokenRemainingLifetimeSeconds, response);
    }

    private static void updateCookie(Cookie cookie, String cookieValue, Integer tokenRemainingLifetimeSeconds,
                                     HttpServletResponse response) {
        if (cookieValue != null) {
            cookie.setValue(cookieValue);
        }
        //Only name and value are sent back to the server from the browser. The other attributes are only used by the browser to determine of the cookie should be sent or not.
        //http://en.wikipedia.org/wiki/HTTP_cookie#Setting_a_cookie
//
        if (tokenRemainingLifetimeSeconds == null) {
            tokenRemainingLifetimeSeconds = DEFAULT_COOKIE_MAX_AGE;
        }
//        cookie.setMaxAge(tokenRemainingLifetimeSeconds);
//
//        if (cookieDomain != null && !cookieDomain.isEmpty()) {
//            cookie.setDomain(cookieDomain);
//        }
//        cookie.setPath("; HttpOnly;");
//        cookie.setSecure(isMyUriSecured);
//        log.debug("Created/updated cookie with name={}, value/userTokenId={}, domain={}, path={}, maxAge={}, secure={}",
//                cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure());
//        response.addCookie(cookie);

        addCookie(cookie.getValue(), tokenRemainingLifetimeSeconds, response);
    }


    public static void clearUserTokenCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = getUserTokenCookie(request);
        if (cookie != null) {
//            cookie.setValue("");
//            cookie.setMaxAge(0);
//            if (cookieDomain != null && !cookieDomain.isEmpty()) {
//                cookie.setDomain(cookieDomain);
//            }
//            //cookie.setPath("/ ; HttpOnly;");
//            //cookie.setPath("/; secure; HttpOnly");
//            cookie.setPath("; HttpOnly;");
//            cookie.setSecure(isMyUriSecured);
////            if ("https".equalsIgnoreCase(request.getScheme())) {
////                cookie.setSecure(true);
////            } else {
////                log.warn("Unsecure session detected, using myuri to define coocie security");
////                cookie.setSecure(secureCookie(myAppUri));
////            }
//            response.addCookie(cookie);
//            log.trace("Cleared cookie with name={}, value/userTokenId={}, domain={}, path={}, maxAge={}, secure={}",
//                    cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure());
            addCookie("", 0, response);
        }
    }

    public static String getUserTokenId(HttpServletRequest request) {
        String userTokenId = request.getParameter(CookieManager.userTokenReferenceName);

        if (userTokenId != null && userTokenId.length() > 1) {
            log.warn("getUserTokenId: userTokenIdFromRequest={}", userTokenId);
        } else {
            userTokenId = CookieManager.getUserTokenIdFromCookie(request);
            log.warn("getUserTokenId: userTokenIdFromCookie={}", userTokenId);
        }
        return userTokenId;
    }
    */

    static String getUserTokenIdFromCookie(HttpServletRequest request) {
        Cookie userTokenCookie = getUserTokenCookie(request);
        String userTokenId = null;

        if (userTokenCookie != null) {
            userTokenId = userTokenCookie.getValue();
        }

        return userTokenId;
    }

    private static Cookie getUserTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            log.debug("getUserTokenCookie: cookie with name={}, value={}",
                    cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath());
            if (userTokenReferenceName.equalsIgnoreCase(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    /*
    public static Cookie getCodeCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            log.debug("getCodeCookie: cookie with name={}, value={}",
                    cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath());
            if ("code".equalsIgnoreCase(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }


    private static void addCookie(String userTokenId,
                                  Integer tokenRemainingLifetimeSeconds, HttpServletResponse response) {
        StringBuilder sb = new StringBuilder(userTokenReferenceName);
        sb.append("=");
        sb.append(userTokenId);
        sb.append(";expires=");
        sb.append(tokenRemainingLifetimeSeconds);
        sb.append(";path=");
        sb.append("/");
        sb.append(";HttpOnly");
        if (isMyUriSecured) {
            sb.append(";secure");
        }
        response.setHeader("SET-COOKIE", sb.toString());
    }
    */
}
