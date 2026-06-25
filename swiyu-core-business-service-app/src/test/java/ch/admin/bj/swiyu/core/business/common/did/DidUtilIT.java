package ch.admin.bj.swiyu.core.business.common.did;

import static ch.admin.bj.swiyu.core.business.common.did.DidUtil.parseIdentifierEntryId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import ch.admin.bj.swiyu.core.business.test.IdentifierTestData;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.eid.did_sidekicks.DidDoc;
import ch.admin.eid.didresolver.DidResolveException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@WithAllTestContainerInitializers
class DidUtilIT {

    @Autowired
    private IdentifierTestData identifierTestData;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void getScid() throws JsonProcessingException {
        var didLog = identifierTestData.validDidTdwLog();
        var scid = DidUtil.getScid(jsonMapper.readTree(didLog));

        assertEquals("QmZrwPndgKMwvBSMzkXHTCy9fHwdmc7tAU1Abs7cUCiHbT", scid);
    }

    @Test
    void getIdV_1_2() {
        var didLog = identifierTestData.validDidTdwLogV1_2_0();
        var did = DidUtil.getId(didLog, jsonMapper);

        assertEquals(
            "did:tdw:QmNPSsZ3DosVyQ1WrwFTpRSPYsqq9YZDLDqizdPSWCJzaS:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:e0e1f8ec-cbba-47dd-abd3-008a111af18a",
            did
        );
    }

    @Test
    void getIdV_2() {
        var didLog = identifierTestData.validDidWebvhLogV2_0_0();
        var did = DidUtil.getId(didLog, jsonMapper);

        assertEquals(
            "did:webvh:QmUMHj7fLuU2tgrDVC4awCqafsDXA7G4KBw1QpYniKKzA2:identifier-reg-d.trust-infra.swiyu.admin.ch:api:v1:did:45d801da-a8f5-4479-8996-a02216d790dc",
            did
        );
    }

    @Test
    void getDidDoc() throws DidResolveException {
        var didLog = identifierTestData.validDidTdwLog();
        var didDoc = DidUtil.getDidDoc(didLog);

        assertNotNull(didDoc);
        assertEquals(DidDoc.class, didDoc.getClass());
        assertEquals(
            "did:tdw:QmZrwPndgKMwvBSMzkXHTCy9fHwdmc7tAU1Abs7cUCiHbT:identifier-reg-r.trust-infra.swiyu.admin.ch:api:v1:did:a2c598f2-cfd5-4cf2-9326-63644edef221",
            didDoc.getId()
        );
    }

    @Test
    void detectDidTdw() {
        var didLog = identifierTestData.validDidTdwLog();
        var method = DidUtil.detectDidMethod(didLog);
        assertNotNull(method);
        assertEquals(DidMethod.DID_TDW, method);
    }

    @Test
    void detectDidWebvh() {
        var didLog = identifierTestData.validDidWebvhEntry();
        var method = DidUtil.detectDidMethod(didLog);
        assertNotNull(method);
        assertEquals(DidMethod.DID_WEBVH, method);
    }

    @Test
    void invalidDidDocShouldThrow_getScid() throws JsonProcessingException {
        assertThrows(IllegalArgumentException.class, () -> DidUtil.getScid(null));
        var emptyObject = jsonMapper.readTree("{}");
        assertThrows(IllegalArgumentException.class, () -> DidUtil.getScid(emptyObject));
        var emptyArray = jsonMapper.readTree("[]");
        assertThrows(IllegalArgumentException.class, () -> DidUtil.getScid(emptyArray));
        var arrayWithNumbers = jsonMapper.readTree("[1, 2]");
        assertThrows(IllegalArgumentException.class, () -> DidUtil.getScid(arrayWithNumbers));

        var missingScid = jsonMapper.readTree("[1, 2, {}]");
        assertThrows(IllegalArgumentException.class, () -> DidUtil.getScid(missingScid));
        var nullScid = jsonMapper.readTree("[1, 2, {\"scid\": null}]");
        assertThrows(IllegalArgumentException.class, () -> DidUtil.getScid(nullScid));

        var blankScid = jsonMapper.readTree("[1, 2, {\"scid\": \"\"}]");
        assertThrows(IllegalArgumentException.class, () -> DidUtil.getScid(blankScid));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{}",
            "[]", // emptyArray
            "[1, 2, 3]", // arrayWithNumbers
            "[1, 2, 3, {}]", // missingId
            "[1, 2, 3, {\"value\": {}}]", // missingValue
            "[1, 2, 3, {\"value\": {\"id\": null}}]", //nullId
            "[1, 2, 3, {\"value\": {\"id\": \"\"}}]", // blankId
        }
    )
    void invalidDidDocShouldThrow_getId(String didLog) {
        assertThrows(IllegalArgumentException.class, () -> DidUtil.getId(didLog, jsonMapper));
    }

    @Test
    void parseIdentifierEntryIdTest() {
        // GIVEN
        var did =
            "did:tdw:QmbBoyVLWetfXMKwsrtZcejKVKhMY5nVy138R7F9bQwxtw:identifier-reg-r.trust-infra.swiyu.admin.ch:api:v1:did:1abc96db-2ade-4b6c-baaf-b4f461cdabed";
        // WHEN / THEN
        assertThat(parseIdentifierEntryId(did)).hasToString("1abc96db-2ade-4b6c-baaf-b4f461cdabed");
    }

    @Test
    void parseIdentifierEntryIdTest_Invalid() {
        // GIVEN
        var did =
            "did:tdw:QmbBoyVLWetfXMKwsrtZcejKVKhMY5nVy138R7F9bQwxtw:identifier-reg-r.trust-infra.swiyu.admin.ch:api:v1:did:A";
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> parseIdentifierEntryId(did));
    }
}
