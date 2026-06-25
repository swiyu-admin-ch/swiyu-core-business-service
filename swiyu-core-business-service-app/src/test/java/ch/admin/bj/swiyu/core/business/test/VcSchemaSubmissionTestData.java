package ch.admin.bj.swiyu.core.business.test;

import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcSchemaSubmission;
import java.util.UUID;

public class VcSchemaSubmissionTestData {

    public static final UUID PARTNER_ID_1 = UUID.fromString("d3502b68-73b5-4d8e-978f-e0c60ffa323c");
    public static final UUID PARTNER_ID_2 = UUID.fromString("e9565a19-a169-455d-91ae-00a3de906101");
    public static final String DEFAULT_FILE = "someFile";

    public static VcSchemaSubmission vcSchemaSubmission(UUID partnerId) {
        return new VcSchemaSubmission(partnerId, DEFAULT_FILE);
    }
}
