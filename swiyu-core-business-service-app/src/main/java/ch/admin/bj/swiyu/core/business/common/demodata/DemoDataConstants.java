package ch.admin.bj.swiyu.core.business.common.demodata;

import lombok.experimental.UtilityClass;

/**
 * Holds constants of reusable Demodata. Some of them are here in common package since we use the values as examples
 * in open-api spec as well.
 */
@UtilityClass
public class DemoDataConstants {

    @UtilityClass
    public static class TrustOnboardingSubmission {

        /**
         * ID of a TrustOnboardingSubmission in state UNSUBMITTED.
         */
        public static final String ID_UNSUBMITTED = "46ada91a-84ce-422b-b9b5-e0d2e3e8c46d";
        /**
         * ID of a TrustOnboardingSubmission in state SUBMITTED.
         */
        public static final String ID_SUCCEEDED = "8369160f-697c-4b12-80d3-91abff1a29ee";
    }
}
