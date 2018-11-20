package net.whydah.service.auth;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;
import org.jose4j.jwk.JsonWebKey;


import static net.whydah.service.CredentialStore.FALLBACK_URL;

@RestController
@Path("jwks")
@Produces(MediaType.APPLICATION_JSON_UTF8_VALUE)
public class JwksEndpointController {
	private final SPAKeyStoreRepository spaKeyStoreRepository;

	@Autowired
	public JwksEndpointController(SPAKeyStoreRepository keystoreRepo) {
		this.spaKeyStoreRepository = keystoreRepo;
	}


	@GET
	public Response getJwks() {
		try {
			String body = spaKeyStoreRepository.getKeystore().toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
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
