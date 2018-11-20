package net.whydah.service.auth;

import net.whydah.sso.user.helpers.UserHelper;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserApplicationRoleEntry;
import net.whydah.sso.user.types.UserToken;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.lang.JoseException;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

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

    @Test
    public void userTokenWithRoles_returnsTokenWithRoles() throws MalformedClaimException {
        UserToken ut = UserTokenMapper.fromUserTokenXml(UserHelper.getDummyUserToken());
        UserApplicationRoleEntry entry1 = new UserApplicationRoleEntry(null, "appid1", "appname1", null, "roleName1", null);
        UserApplicationRoleEntry entry2 = new UserApplicationRoleEntry(null, "appid2", "appname2", null, "roleName2", null);
        UserApplicationRoleEntry entry3 = new UserApplicationRoleEntry(null, "appid2", "appname2", null, "roleName3", null);
        UserApplicationRoleEntry entry4 = new UserApplicationRoleEntry(null, "appid2", "appname2", null, "roleName4", null);
        ut.setRoleList(Arrays.asList(entry1, entry2, entry3, entry4));
        RsaJsonWebKey rsaKey = RsaJwkHelper.produce();

        String jwt = AdvancedJWTokenUtil.buildJWT(rsaKey, ut, UUID.randomUUID().toString(), null);

        JwtClaims jwtClaims = AdvancedJWTokenUtil.parseJWT(jwt, rsaKey.getKey());
        assertNotNull(jwtClaims);

        @SuppressWarnings("unchecked")
        List<String> appname1 = jwtClaims.getClaimValue("appname1", List.class);
        assertEquals(appname1.size(), 1);
        assertTrue(appname1.contains("roleName1"));

        @SuppressWarnings("unchecked")
        List<String> appname2 = jwtClaims.getClaimValue("appname2", List.class);
        assertEquals(appname2.size(), 3);
        assertTrue(appname2.containsAll(Arrays.asList("roleName2", "roleName3", "roleName4")));
    }
}
