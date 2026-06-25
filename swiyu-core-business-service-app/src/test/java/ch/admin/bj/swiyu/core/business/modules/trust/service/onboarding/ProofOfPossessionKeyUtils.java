package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import net.minidev.json.JSONObject;

public class ProofOfPossessionKeyUtils {

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        var kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        return kpg.generateKeyPair();
    }

    public static ECDSASigner getSigner(PrivateKey privateKey) throws JOSEException {
        return new ECDSASigner((ECPrivateKey) privateKey);
    }

    public static ECDSAVerifier getVerifier(PublicKey publicKey) throws JOSEException {
        return new ECDSAVerifier((ECPublicKey) publicKey);
    }

    public static String getPoPSubmission(String nonce, String did, ECDSASigner signer) throws JOSEException {
        return getPoPSubmission(nonce, did, did + "#suffix", signer, Instant.now(), Instant.now().plusSeconds(3600));
    }

    public static String getPoPSubmission(String nonce, String did, String keyId, ECDSASigner signer)
        throws JOSEException {
        return getPoPSubmission(nonce, did, keyId, signer, Instant.now(), Instant.now().plusSeconds(3600));
    }

    /**
     * Creates a signed PoP submission JWT based on the EIDARTFE-959 feature specification.
     * @param nonce UUID nonce to include in the JWT
     * @param did DID to include in the JWT (as key ID) without suffix
     * @param signer private key signer
     * @param issuedAt issued at time for the jwt
     * @param expiresAt expiration time for the jwt
     * @return the serialized JWT
     */
    public static String getPoPSubmission(
        String nonce,
        String did,
        String keyId,
        ECDSASigner signer,
        Instant issuedAt,
        Instant expiresAt
    ) throws JOSEException {
        JSONObject payload = new JSONObject();
        payload.put("exp", (int) expiresAt.getEpochSecond()); // 1 hour expiration
        payload.put("nonce", nonce);
        payload.put("iat", (int) issuedAt.getEpochSecond());
        payload.put("iss", did);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build();
        JWSObject jws = new JWSObject(header, new Payload(payload));
        jws.sign(signer);
        return jws.serialize();
    }
}
