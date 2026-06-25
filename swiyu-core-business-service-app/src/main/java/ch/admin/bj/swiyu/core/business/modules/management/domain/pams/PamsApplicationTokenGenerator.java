package ch.admin.bj.swiyu.core.business.modules.management.domain.pams;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import lombok.experimental.UtilityClass;

/**
 * Generator for PAMS authentication token
 * Link: https://confluence.bit.admin.ch/x/phhhHQ
 */
@UtilityClass
class PamsApplicationTokenGenerator {

    private static final String API_ID_CLAIM = "tid";
    private static final String DEFAULT_APP_ID_CLAIM = "cid";
    private static final String USER_AGENT_CLAIM = "user_agent";

    public static String createToken(
        int appId,
        int apiId,
        String issuedBy,
        Duration timeToLive,
        String userAgent,
        JWK key
    ) throws JOSEException {
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(key.getKeyID()).build();
        var payload = new JWTClaimsSet.Builder();
        var now = Instant.now();

        payload.claim(DEFAULT_APP_ID_CLAIM, appId);
        payload.claim("v", 2);
        payload.issuer(issuedBy);
        payload.issueTime(Date.from(now));
        payload.expirationTime(Date.from(now.plus(timeToLive)));

        payload.claim(API_ID_CLAIM, apiId);
        payload.claim(USER_AGENT_CLAIM, userAgent);

        var signedJWT = new SignedJWT(header, payload.build());
        signedJWT.sign(new RSASSASigner(key.toRSAKey()));
        return signedJWT.serialize();
    }
}
