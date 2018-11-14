package net.whydah.service.auth;

import javax.ws.rs.core.Response;

final class UserResponseUtil {
    private UserResponseUtil() {
    }

    static Response createResponseWithHeader(String data, String redirectUrl) {
        return Response.ok(data)
                .header("Access-Control-Allow-Origin", redirectUrl)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }

    static Response createForbiddenResponseWithHeader(String redirectUrl) {
        return Response.status(Response.Status.FORBIDDEN)
                .header("Access-Control-Allow-Origin", redirectUrl)
                .header("Access-Control-Allow-Credentials", true)
                .header("Access-Control-Allow-Headers", "*")
                .build();
    }
}
