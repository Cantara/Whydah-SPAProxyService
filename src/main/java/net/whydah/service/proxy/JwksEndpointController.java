package net.whydah.service.proxy;

import static net.whydah.service.CredentialStore.FALLBACK_URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.jose4j.jwk.JsonWebKey;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;

import net.whydah.util.RsaJwkHelper;


@RestController
@Path("jwks")
@Produces(MediaType.APPLICATION_JSON_UTF8_VALUE)
public class JwksEndpointController {

	@GET
	public Response getJwks() {

		try {
			String body = RsaJwkHelper.loadJWKS().toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
			return Response.ok(body).build();
		} catch(Exception ex) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.header("Location", FALLBACK_URL)
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Credentials", true)
					.header("Access-Control-Allow-Headers", "*")
					.build();
		}
	}
	
}
