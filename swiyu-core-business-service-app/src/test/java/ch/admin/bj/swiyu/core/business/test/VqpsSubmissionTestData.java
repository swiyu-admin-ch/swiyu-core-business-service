package ch.admin.bj.swiyu.core.business.test;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsPublicationFailureReasonDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsPublicationResultDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionB2BDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionCreateRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionStatusDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vqps.VqpsSubmission;
import ch.admin.bj.swiyu.messagetype.ti.TiVqpsPublicationSucceededEvent;
import ch.admin.bj.swiyu.messagetype.ti.VqpsPublicationSucceededPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;

@UtilityClass
public class VqpsSubmissionTestData {

    public static VqpsSubmissionCreateRequestDto vqpsSubmissionCreateRequestDto() {
        return new VqpsSubmissionCreateRequestDto(
            true,
            "someSub",
            localizedMap(),
            purposeDescriptionMap(),
            "com.example.identityCardCredential_presentation",
            dqclQuery()
        );
    }

    public static VqpsSubmissionCreateRequestDto vqpsSubmissionCreateRequestDto(
        String sub,
        Map<String, String> purposeName,
        Map<String, String> purposeDescription,
        String scope,
        JsonNode query
    ) {
        return new VqpsSubmissionCreateRequestDto(true, sub, purposeName, purposeDescription, scope, query);
    }

    public static Map<String, String> localizedMap(String defaultValue, Map<String, String> localized) {
        var map = new java.util.LinkedHashMap<String, String>();
        map.put("default", defaultValue);
        map.putAll(localized);
        return Map.copyOf(map);
    }

    public static VqpsSubmissionB2BDto vqpsSubmissionB2BDto(UUID id, VqpsSubmissionStatusDto status) {
        return switch (status) {
            case ACCEPTED -> new VqpsSubmissionB2BDto(
                id,
                UUID.randomUUID(),
                0L,
                status,
                null,
                null,
                Instant.now(),
                Instant.now()
            );
            case PUBLICATION_SUCCEEDED -> new VqpsSubmissionB2BDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                status,
                new VqpsPublicationResultDto(
                    "07f289d5-8b1f-4604-bf72-53bdcb71ee05",
                    "ey...",
                    Instant.now().plusSeconds(100)
                ),
                null,
                Instant.now(),
                Instant.now()
            );
            case PUBLICATION_FAILED -> new VqpsSubmissionB2BDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1L,
                status,
                null,
                VqpsPublicationFailureReasonDto.UNKNOWN,
                Instant.now(),
                Instant.now()
            );
        };
    }

    public static @NonNull TiVqpsPublicationSucceededEvent tiVqpsPublicationSucceededEvent(UUID submissionId) {
        String vqpsJwt;
        try {
            vqpsJwt = vqpsJwt(UUID.randomUUID(), Instant.now().plusSeconds(3600));
        } catch (JOSEException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }

        var event = new TiVqpsPublicationSucceededEvent();
        event.setPublisher(
            AvroDomainEventPublisher.newBuilder().setService("swiyu-trust-management-scs").setSystem("ti").build()
        );
        event.setPayload(
            VqpsPublicationSucceededPayload.newBuilder().setVqps(vqpsJwt).setVqpsSubmissionId(submissionId).build()
        );
        return event;
    }

    private static @Valid @NotNull JsonNode dqclQuery() {
        var json = dqlcQueryString();
        var mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, JsonNode.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("failed to parse json", e);
        }
    }

    public static VqpsSubmission vqpsSubmission(UUID partnerId) {
        return new VqpsSubmission(
            partnerId,
            "someSub",
            localizedMap(),
            purposeDescriptionMap(),
            "com.example.identityCardCredential_presentation",
            dqclQuery()
        );
    }

    /**
     * Returns a Verification Query Public Statements (signed jwt).
     */
    public static String vqpsJwt(UUID jti, Instant expiresAt)
        throws JOSEException, InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        // Generate EC key pair (secp256r1 = P-256, matches ES256)
        var kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        var keyPair = kpg.generateKeyPair();

        // Build header with custom claims
        var header = new JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(new JOSEObjectType("swiyu-verification-query-public-statement+jwt"))
            .keyID("did:example:verification-statment-issuer#key-1")
            .customParam("profile_version", "swiss-profile-trust:1.0.0")
            .build();

        // Build claims
        var claims = new JWTClaimsSet.Builder()
            .jwtID(jti.toString())
            .subject("did:example:verifier")
            .issueTime(Date.from(expiresAt.minusSeconds(10)))
            .expirationTime(Date.from(expiresAt))
            .claim("purpose_name", "beispiel abfrage")
            .claim("purpose_name#de-ch", "beispiel abfrage")
            .claim("purpose_description", "frage ab zum beispiel")
            .claim("purpose_description#de-ch", "frage ab zum beispiel")
            .claim(
                "request",
                Map.of("type", "DCQL", "scope", "com.example.identityCardCredential_presentation", "query", dqclQuery())
            )
            .build();

        var signedJWT = new SignedJWT(header, claims);
        signedJWT.sign(new ECDSASigner((ECPrivateKey) keyPair.getPrivate()));
        return signedJWT.serialize();
    }

    private static @NonNull String dqlcQueryString() {
        return """
          {
          "credentials": [
            {
              "id": "my_credential",
              "format": "dc+sd-jwt",
              "meta": {
                "vct_values": [ "https://credentials.example.com/identity_credential" ]
              },
              "claims": [
                  {"path": ["last_name"]},
                  {"path": ["first_name"]},
                  {"path": ["address", "street_address"]}
              ]
            }
          ]
        }
        """;
    }

    private static @NonNull Map<String, @NotBlank @Size(max = 1000) String> purposeDescriptionMap() {
        return Map.of(
            "default",
            "purpose description",
            "de-CH",
            "purpose description de",
            "fr-CH",
            "purpose description fr",
            "it-CH",
            "purpose description it",
            "rm-CH",
            "purpose description rm"
        );
    }

    private static @NonNull Map<String, @NotBlank @Size(max = 40) String> localizedMap() {
        return Map.of(
            "default",
            "purpose name",
            "de-CH",
            "purpose name de",
            "fr-CH",
            "purpose name fr",
            "it-CH",
            "purpose name it",
            "rm-CH",
            "purpose name rm"
        );
    }
}
