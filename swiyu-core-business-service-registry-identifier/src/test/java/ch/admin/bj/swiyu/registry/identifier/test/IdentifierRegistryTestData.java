package ch.admin.bj.swiyu.registry.identifier.test;

import ch.admin.bj.swiyu.registry.identifier.domain.DatastoreStatus;
import ch.admin.bj.swiyu.registry.identifier.domain.DidEntity;
import ch.admin.bj.swiyu.registry.identifier.domain.DidType;
import ch.admin.bj.swiyu.registry.identifier.domain.IdentifierDatastoreEntity;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IdentifierRegistryTestData {

    public static DidEntity didEntityTdw(IdentifierDatastoreEntity datastoreEntity) {
        return new DidEntity(datastoreEntity, DidType.DID_TDW, "dummy_json_log", "TEST_READ");
    }

    public static DidEntity didEntityWeb(IdentifierDatastoreEntity datastoreEntity) {
        return new DidEntity(datastoreEntity, DidType.DID_WEB, "{\"json\":\"code\"}", "TEST_READ");
    }

    public static IdentifierDatastoreEntity datastoreEntity(DatastoreStatus status) {
        var entity = new IdentifierDatastoreEntity(UUID.randomUUID());
        entity.changeStatus(status);
        return entity;
    }
}
