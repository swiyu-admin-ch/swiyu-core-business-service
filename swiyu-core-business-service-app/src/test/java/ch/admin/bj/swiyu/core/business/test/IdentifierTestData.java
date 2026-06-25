package ch.admin.bj.swiyu.core.business.test;

import static ch.admin.bj.swiyu.core.business.common.did.DidUtil.parseIdentifierEntryId;

import ch.admin.bj.swiyu.core.business.common.utils.FileUtil;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntry;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Provides the test data for identifier tests
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings(
    {
        "java:S116", // Due to multiple relevant version numbers field names do not need to comply with default naming conventions
    }
)
public class IdentifierTestData {

    @Value("classpath:data/identifier/didtoolbox/1.2.0/valid.jsonl")
    private Resource validDidTdwLogV1_2_0;

    @Value("classpath:data/identifier/didtoolbox/2.0.0/valid.jsonl")
    private Resource validDidWebvhLogV2_0_0;

    @Value("classpath:data/identifier/didtoolbox/2.1.0/valid.jsonl")
    private Resource validDidWebvhLogV2_1_0;

    @Value("classpath:data/identifier/didtoolbox/1.0.0/valid.jsonl")
    private Resource validDidTdwLog;

    @Value("classpath:data/identifier/didtoolbox/1.0.0/invalid_data_integrity.jsonl")
    private Resource invalidDidTdwLogDataIntegrity;

    @Value("classpath:data/identifier/didtoolbox/1.0.0/invalid_json.jsonl")
    private Resource invalidJsonl;

    @Value("classpath:data/identifier/didtoolbox/1.0.0/invalid_portable.jsonl")
    private Resource invalidDidLogPortable;

    @Value("classpath:data/identifier/didtoolbox/1.7/webvh/1.0/invalid_wrong_base_register.jsonl")
    private Resource invalidDidWebvhWrongBaseRegister;

    @Value("classpath:data/identifier/didtoolbox/1.7/webvh/1.0/valid.jsonl")
    private Resource validDidWebvhEntry;

    @Value("classpath:data/identifier/didtoolbox/1.7/webvh/1.0/valid_updated.jsonl")
    private Resource validDidWebvhUpdatedEntry;

    public static void insertTestIdentifierEntries(IdentifierEntryRepository identifierEntryRepository) {
        identifierEntryRepository.deleteAllInBatch();
        identifierEntryRepository.save(
            new IdentifierEntry(StatusTestData.DID_ENTRY_ID_FROM_ISSUER_A, BusinessEntityTestData.ENTITY_A)
        );
    }

    public static IdentifierEntry identifierEntry(UUID businessPartnerId) {
        return new IdentifierEntry(UUID.randomUUID(), businessPartnerId);
    }

    public static IdentifierEntry identifierEntry_Initialized(UUID businessPartnerId, String did) {
        var entity = new IdentifierEntry(parseIdentifierEntryId(did), businessPartnerId);
        entity.updateDidAndActivate(did);
        return entity;
    }

    public String validDidTdwLog() {
        return FileUtil.asString(validDidTdwLog);
    }

    public String validDidTdwLogV1_2_0() {
        return FileUtil.asString(validDidTdwLogV1_2_0);
    }

    public String validDidWebvhLogV2_0_0() {
        return FileUtil.asString(validDidWebvhLogV2_0_0);
    }

    public String validDidWebvhLogV2_1_0() {
        return FileUtil.asString(validDidWebvhLogV2_1_0);
    }

    public String invalidDidTdwLogDataIntegrity() {
        return FileUtil.asString(invalidDidTdwLogDataIntegrity);
    }

    public String invalidJsonl() {
        return FileUtil.asString(invalidJsonl);
    }

    public String invalidDidLogPortable() {
        return FileUtil.asString(invalidDidLogPortable);
    }

    public String invalidDidWebvhWrongBaseRegister() {
        return FileUtil.asString(invalidDidWebvhWrongBaseRegister);
    }

    public String validDidWebvhEntry() {
        return FileUtil.asString(validDidWebvhEntry);
    }

    public String validDidWebvhUpdatedEntry() {
        return FileUtil.asString(validDidWebvhUpdatedEntry);
    }
}
