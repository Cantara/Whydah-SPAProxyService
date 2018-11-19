package net.whydah.service.auth;

import net.whydah.sso.user.helpers.UserHelper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.lang.JoseException;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class AdvancedJWTUtilTest {

	@Test
	public void testVerifyingJWT() throws JoseException {
		//create a new JWT with a new key
		UserToken ut = UserTokenMapper.fromUserTokenXml(UserHelper.getDummyUserToken());
		RsaJsonWebKey rsaKey = RsaJwkHelper.produce();
		String jwt = AdvancedJWTokenUtil.buildJWT(rsaKey, ut, UUID.randomUUID().toString(), "SOME_APP");

		//some wrong key
		JsonWebKey jwk = JsonWebKey.Factory.newJwk("{\"kty\":\"RSA\",\"n\":\"gvAb4pR0iLcLnvbPVDZ4pjzq7IGTzLJhVPyVxUsb1DalooopDuRGdz0IQH3jNaPbmoFcGCYKc9Tg234LURu35HIvUza3v5yCGzTUsNEZ8bUcYSFe569DJORVYdN9bkhmCaADjD4oYDcGu8HXGPpa8yk9-9jiqsNnok0iTSI1KuIAshbprdI-vgNlQD81nOToSb9lqZhpe66aLObLmg3XOfh4FfezQocslgu3wyfhTzgqza9jYmgbXSVNsVsUms9d6gy2DKnhqV42Opvfs9YtBLTTgYmHGJbMvA05Y74Up31_WJChi4VE6HGmaKS_GMYOHWtCwDYgCypPh0Z6qcVb8Q\",\"e\":\"AQAB\"}");
		assertNull(AdvancedJWTokenUtil.parseJWT(jwt, jwk.getKey()));
		//right key
		assertNotNull(AdvancedJWTokenUtil.parseJWT(jwt, rsaKey.getKey()));

	}
}
