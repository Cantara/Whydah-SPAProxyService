package net.whydah.service.auth;

import javax.ws.rs.core.Response;

public final class UserResponseUtil {
    private UserResponseUtil() {
    }

    public static Response createResponseWithHeader(String data, String redirectUrl) {
        return Response.ok(data)
                .header("Access-Control-Allow-Origin", redirectUrl)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }

}
