package ch.admin.bj.swiyu.core.business.common.features;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Data
@Validated
@ConfigurationProperties(prefix = "features")
public final class FeaturesProperties {

    @NotNull
    private Boolean eid5540UpdateIsGovernment;

    @NotNull
    private Boolean eidartfe754VcSchema;

    @NotNull
    private Boolean eidartfe1220ProofOfPossession;

    @NotNull
    private Boolean eidartfe1204TrustAddDids;

    @PostConstruct
    public void logFeatureFlags() {
        log.info(
            """
            Following features are configured:
              EID_5540_UPDATE_IS_GOVERNMENT:{},
              EIDARTFE_754_VC_SCHEMA:{},
              EIDARTFE_1220_PROOF_OF_POSSESSION:{},
              EIDARTFE_1204_TRUST_ADD_DIDS:{}
            """,
            eid5540UpdateIsGovernment,
            eidartfe754VcSchema,
            eidartfe1220ProofOfPossession,
            eidartfe1204TrustAddDids
        );
    }
}
