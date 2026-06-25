package ch.admin.bj.swiyu.core.business.modules.status.service;

import static ch.admin.bj.swiyu.core.business.common.did.DidUtil.parseIdentifierEntryId;

import ch.admin.bj.swiyu.core.business.common.did.CryptoIntegrityValidator;
import ch.admin.bj.swiyu.core.business.common.exceptions.CryptoIntegrityValidationFailedException;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.status.config.StatusListsLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.status.domain.StatusListEntry;
import ch.admin.bj.swiyu.core.business.modules.status.exceptions.StatusListValidationFailedException;
import ch.admin.bj.swiyu.registry.status.service.StatusListRegistryService;
import ch.admin.eid.didresolver.DidKt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.networknt.schema.JsonSchema;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.SignedJWT;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.InflaterInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusListValidator {

    private static final int MAX_DECOMPRESSED_STATUS_LIST_SIZE_BYTES = 200 * 1024;
    private static final String REQUIRED_TYP = "statuslist+jwt";
    private static final String REQUIRED_PROFILE_VERSION = "swiss-profile-vc:1.0.0";

    private final JsonMapper schemaValidatorObjectMapper;
    private final JsonSchema statusListSchema;
    private final StatusListsLimitProperties statusListsLimitProperties;
    private final StatusListRegistryService statusListRegistryService;
    private final IdentifierEntryService identifierEntryService;
    private final CryptoIntegrityValidator cryptoIntegrityValidator;

    public void validateStatusListVcV2(StatusListEntry entry, String rawStatusListVc)
        throws StatusListValidationFailedException {
        if (rawStatusListVc == null || rawStatusListVc.isEmpty()) {
            throw new StatusListValidationFailedException("Status list VC is null or empty", null);
        }

        checkStatusListVcSize(rawStatusListVc);

        SignedJWT statusListVc;
        try {
            statusListVc = SignedJWT.parse(rawStatusListVc);

            checkTypHeader(statusListVc.getHeader());
            checkProfileVersionHeader(statusListVc.getHeader());

            checkStatusListCryptoIntegrity(statusListVc);
            checkStatusListConformsToSchema(statusListVc);

            checkExpClaimIsSet(statusListVc);
            checkStatusListIsNewlyCreated(statusListVc);

            checkDecompressedStatusList(statusListVc);

            if (entry.getUploadCount() == 0) {
                checkStatusListBelongsToBusinessPartner(entry, statusListVc);
            } else {
                checkStatusListIsFromSameIssuer(entry, statusListVc);
            }
        } catch (ParseException e) {
            throw new StatusListValidationFailedException("Statuslist VC could not be parsed.", e);
        }
    }

    public void validateStatusListVc(StatusListEntry entry, String rawStatusListVc)
        throws StatusListValidationFailedException {
        if (rawStatusListVc == null || rawStatusListVc.isEmpty()) {
            throw new StatusListValidationFailedException("Status list VC is null or empty", null);
        }

        checkStatusListVcSize(rawStatusListVc);

        SignedJWT statusListVc;
        try {
            statusListVc = SignedJWT.parse(rawStatusListVc);

            checkStatusListCryptoIntegrity(statusListVc);
            checkStatusListConformsToSchema(statusListVc);
            checkStatusListIsNewlyCreated(statusListVc);
            if (entry.getUploadCount() == 0) {
                checkStatusListBelongsToBusinessPartner(entry, statusListVc);
            } else {
                checkStatusListIsFromSameIssuer(entry, statusListVc);
            }
        } catch (ParseException e) {
            throw new StatusListValidationFailedException("Statuslist VC could not be parsed.", e);
        }
    }

    private void checkStatusListIsFromSameIssuer(StatusListEntry entry, SignedJWT statusListVc) throws ParseException {
        var oldStatusListRaw = statusListRegistryService.getStatusListVc(entry.getStatusRegistryEntryId());
        if (oldStatusListRaw == null || oldStatusListRaw.isEmpty()) {
            log.warn(
                "Statuslist VC could not be retrieved from the server. Statuslist URL: {}",
                statusListVc.getJWTClaimsSet().getSubject()
            );
            throw new StatusListValidationFailedException("Old statuslist VC could not be resolved.", null);
        }
        var oldStatusList = SignedJWT.parse(oldStatusListRaw);
        String oldIssuer;
        String newIssuer;
        try (
            var oldDid = DidKt.getDidFromAbsoluteKid(oldStatusList.getHeader().getKeyID());
            var newDid = DidKt.getDidFromAbsoluteKid(statusListVc.getHeader().getKeyID())
        ) {
            oldIssuer = oldDid.asString();
            newIssuer = newDid.asString();
        } catch (ch.admin.eid.didresolver.DidResolveException e) {
            throw new StatusListValidationFailedException("Statuslist VC kid cannot be resolved to an issuer DID.", e);
        }
        if (!oldIssuer.equals(newIssuer)) {
            throw new StatusListValidationFailedException(
                "Statuslist VC issuer does not match the issuer of the already uploaded statuslist.",
                null
            );
        }
    }

    private void checkStatusListIsNewlyCreated(SignedJWT statusListVc) throws ParseException {
        if (
            statusListVc
                .getJWTClaimsSet()
                .getIssueTime()
                .toInstant()
                .compareTo(Instant.now().minus(statusListsLimitProperties.maxAge())) <
            0
        ) {
            throw new StatusListValidationFailedException("Statuslist VC is too old.", null);
        }
    }

    private void checkStatusListBelongsToBusinessPartner(StatusListEntry entry, SignedJWT statusListVc) {
        String issuerDID;
        try (var did = DidKt.getDidFromAbsoluteKid(statusListVc.getHeader().getKeyID())) {
            issuerDID = did.asString();
        } catch (ch.admin.eid.didresolver.DidResolveException e) {
            throw new StatusListValidationFailedException("Statuslist VC kid cannot be resolved to an issuer DID.", e);
        }
        UUID identifierEntryId;
        try {
            identifierEntryId = parseIdentifierEntryId(issuerDID);
        } catch (IllegalArgumentException e) {
            throw new StatusListValidationFailedException("Statuslist VC issuer is not a valid business partner.", e);
        }
        if (
            !identifierEntryService.belongsIdentifierToBusinessPartner(identifierEntryId, entry.getBusinessEntityId())
        ) {
            throw new StatusListValidationFailedException(
                "Statuslist VC is not signed by an issuer belonging to the same business partner.",
                null
            );
        }
    }

    private void checkStatusListConformsToSchema(SignedJWT statusListVc) {
        try {
            var payload = statusListVc.getPayload().toString();

            JsonNode statusVc = schemaValidatorObjectMapper.readTree(payload);
            var validationResult = statusListSchema.validate(statusVc);
            if (!validationResult.isEmpty()) {
                throw new StatusListValidationFailedException(validationResult.toString(), null);
            }
        } catch (JsonProcessingException e) {
            throw new StatusListValidationFailedException(e.getMessage(), e);
        }
    }

    void checkStatusListCryptoIntegrity(SignedJWT statusListVc) throws StatusListValidationFailedException {
        try {
            cryptoIntegrityValidator.checkJwtCryptoIntegrity(statusListVc);
        } catch (CryptoIntegrityValidationFailedException e) {
            throw new StatusListValidationFailedException(String.join("", e.getAdditionalDetails()), e);
        }
    }

    private void checkStatusListVcSize(String statusListVc) throws StatusListValidationFailedException {
        long minSizeBytes = statusListsLimitProperties.minSize().toBytes();
        long maxSizeKilobytes = statusListsLimitProperties.maxSize().toKilobytes();
        long maxSizeBytes = statusListsLimitProperties.maxSize().toBytes();

        var statusListVcByteArray = statusListVc.getBytes(StandardCharsets.UTF_8);
        var actualSizeBytes = statusListVcByteArray.length;

        if (actualSizeBytes <= minSizeBytes || actualSizeBytes >= maxSizeBytes) {
            var message = String.format(
                "Status list VC size must be greater than %d bytes and less than %d kilobytes. Actual size: %d bytes.",
                minSizeBytes,
                maxSizeKilobytes,
                actualSizeBytes
            );
            throw new StatusListValidationFailedException(message, null);
        }
    }

    private void checkTypHeader(JWSHeader header) {
        var typ = header.getType();
        if (typ == null || !REQUIRED_TYP.equals(typ.getType())) {
            throw new StatusListValidationFailedException(
                "JWT typ header must be '" + REQUIRED_TYP + "' but was: " + (typ == null ? "null" : typ.getType()),
                null
            );
        }
    }

    private void checkProfileVersionHeader(JWSHeader header) {
        var profileVersion = header.getCustomParam("profile_version");
        if (!REQUIRED_PROFILE_VERSION.equals(profileVersion)) {
            throw new StatusListValidationFailedException(
                "JWT profile_version header must be '" + REQUIRED_PROFILE_VERSION + "' but was: " + profileVersion,
                null
            );
        }
    }

    private void checkExpClaimIsSet(SignedJWT statusListVc) throws ParseException {
        if (statusListVc.getJWTClaimsSet().getExpirationTime() == null) {
            throw new StatusListValidationFailedException("exp claim must be set in the status list JWT", null);
        }
    }

    private void checkDecompressedStatusList(SignedJWT statusListVc) throws ParseException {
        var statusListClaim = statusListVc.getJWTClaimsSet().getJSONObjectClaim("status_list");
        if (statusListClaim == null) {
            throw new StatusListValidationFailedException("status_list claim must be set in the status list JWT", null);
        }
        if (!statusListClaim.containsKey("lst")) {
            throw new StatusListValidationFailedException("lst claim must be set in the status list JWT", null);
        }
        if (!(statusListClaim.get("lst") instanceof String lst)) {
            throw new StatusListValidationFailedException("lst claim must be a string in the status list JWT", null);
        }

        byte[] compressedData;
        try {
            compressedData = Base64.getUrlDecoder().decode(lst);
        } catch (IllegalArgumentException e) {
            throw new StatusListValidationFailedException("lst claim is not valid Base64url", e);
        }

        var totalRead = 0;
        var buffer = new byte[4096];
        try (var inflater = new InflaterInputStream(new ByteArrayInputStream(compressedData))) {
            int bytesRead;
            while ((bytesRead = inflater.read(buffer)) != -1) {
                totalRead += bytesRead;
                if (totalRead > MAX_DECOMPRESSED_STATUS_LIST_SIZE_BYTES) {
                    throw new StatusListValidationFailedException(
                        "Decompressed status list exceeds the size limit",
                        null
                    );
                }
            }

            if (!(statusListClaim.get("bits") instanceof Number)) {
                throw new StatusListValidationFailedException(
                    "bits claim must be a number in the status list JWT",
                    null
                );
            }
            var bitsPerEntry = ((Number) statusListClaim.get("bits")).intValue();
            var totalBits = (long) totalRead * 8;
            if (bitsPerEntry > 0 && totalBits % bitsPerEntry != 0) {
                throw new StatusListValidationFailedException(
                    "Decompressed status list bit count is not aligned with the bits-per-entry value (" +
                        bitsPerEntry +
                        ")",
                    null
                );
            }
        } catch (StatusListValidationFailedException e) {
            throw e;
        } catch (IOException e) {
            throw new StatusListValidationFailedException("Failed to decompress the status list lst claim", e);
        }
    }
}
