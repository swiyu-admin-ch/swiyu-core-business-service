package ch.admin.bj.swiyu.core.business.common.did;

import ch.admin.eid.did_sidekicks.DidDoc;
import ch.admin.eid.didresolver.Did;
import ch.admin.eid.didresolver.DidResolveException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.validation.constraints.NotNull;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DidUtil {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    public static String getScid(JsonNode didLogEntry) {
        try {
            return getScidFromDidWebvh(didLogEntry);
        } catch (IllegalArgumentException _) {
            return getScidFromDidTdw(didLogEntry);
        }
    }

    public static String getScidFromDidWebvh(JsonNode didLogEntry) {
        if (didLogEntry == null || !didLogEntry.isObject()) {
            throw new IllegalArgumentException(
                "Malformed didLogEntry: DIDLog needs to conform to did:webvh. Missing scid"
            );
        }
        JsonNode scidNode = didLogEntry.path("parameters").path("scid");
        if (scidNode.isMissingNode() || scidNode.isNull()) {
            throw new IllegalArgumentException("Missing required 'scid' in first entry of DIDLog");
        }
        String scid = scidNode.asText();
        if (scid.isBlank()) {
            throw new IllegalArgumentException("'scid' is present but blank");
        }
        return scid;
    }

    public static String getScidFromDidTdw(JsonNode didLogEntry) {
        if (didLogEntry == null || !didLogEntry.isArray() || didLogEntry.size() <= 2) {
            throw new IllegalArgumentException(
                "Malformed didLogEntry: parameters element (index 2) is missing or invalid"
            );
        }
        JsonNode scidNode = didLogEntry.get(2).path("scid");
        if (scidNode.isMissingNode() || scidNode.isNull()) {
            throw new IllegalArgumentException("Missing required 'scid' in didLogEntry[2]");
        }
        String scid = scidNode.asText();
        if (scid.isBlank()) {
            throw new IllegalArgumentException("'scid' is present but blank");
        }
        return scid;
    }

    /**
     * Get ID of a DIDLog (aka the DID)
     */
    public static String getId(@NotNull String didLog, JsonMapper jsonMapper) {
        var firstDidLogEntry = didLog
            .lines()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("DIDLog needs at least one line."));

        try {
            jsonMapper.readTree(firstDidLogEntry);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("DIDLogEntry needs to be JSON formatted.", e);
        }
        String did;
        try {
            did = DidUtil.getDidDoc(didLog).getId();
        } catch (DidResolveException e) {
            throw new IllegalArgumentException("DIDLogEntry is not valid.", e);
        }
        return did;
    }

    public static DidDoc getDidDoc(String didLog) throws DidResolveException {
        // didresolver >= 2.8.x requires that the DID string passed to new Did("...") exactly matches
        // the id field in the resolved DID document. Before that, we passed a placeholder
        // ("url.not.required") which no longer works. We therefore extract the real DID from the
        // log itself. Jackson's readTree() stops after the first complete JSON value, giving us the first entry.
        JsonNode firstEntry;
        try {
            firstEntry = MAPPER.readTree(didLog);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot parse first DIDLog entry", e);
        }
        var method = detectDidMethod(didLog);
        try (var did = new Did(extractDid(method, firstEntry))) {
            return did.resolveAll(didLog).getDidDoc();
        }
    }

    public static URL getDidUrl(DidDoc didDoc) throws DidResolveException, URISyntaxException, MalformedURLException {
        try (var did = new Did(didDoc.getId())) {
            return new URI(did.getUrl()).toURL();
        }
    }

    public static String getDidFromKeyId(String keyId) {
        var matcher = Pattern.compile("^(?<did>[^#]+)(?:#.*)?$").matcher(keyId);
        if (matcher.matches()) {
            return matcher.group("did");
        }
        return null;
    }

    /**
     * Detects the DID method from the first line of the DIDLog.
     * Throws if neither did:tdw nor did:webvh is found.
     */
    public static DidMethod detectDidMethod(String didLogEntry) {
        if (didLogEntry.contains("did:tdw:")) {
            return DidMethod.DID_TDW;
        }
        if (didLogEntry.contains("did:webvh:")) {
            return DidMethod.DID_WEBVH;
        }
        throw new IllegalArgumentException(
            "Unknown DID method in log entry: neither 'did:tdw:' nor 'did:webvh:' found"
        );
    }

    /**
     * Parses the identifier entry ID from a DID. The DID is expected to end with the UUID of the identifier entry.
     * Example: did:web:registry.admin.ch:0000-0000-0000-00000 -> 0000-0000-0000-00000
     */
    public static UUID parseIdentifierEntryId(String did) {
        try {
            var didSplitted = did.split(":");
            return UUID.fromString(didSplitted[didSplitted.length - 1]);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("DID does not contain an identifier entry ID", e);
        }
    }

    // Both DID log formats embed the full DID in their first log entry, but in different places:
    // - did:webvh 1.0 (JSON object): state.id     — https://identity.foundation/didwebvh/v1.0/
    // - did:tdw   0.3 (JSON array):  [3].value.id — https://identity.foundation/didwebvh/v0.3/
    // (did:tdw was the original name, renamed to did:webvh from v0.5 onwards)
    private static String extractDid(DidMethod method, JsonNode firstEntry) {
        String id = switch (method) {
            case DID_TDW -> firstEntry.path(3).path("value").path("id").asText(null);
            case DID_WEBVH -> firstEntry.path("state").path("id").asText(null);
        };
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Cannot extract DID from first log entry");
        }
        return id;
    }
}
