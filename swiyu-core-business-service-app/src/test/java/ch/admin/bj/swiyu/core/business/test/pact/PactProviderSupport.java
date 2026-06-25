package ch.admin.bj.swiyu.core.business.test.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class PactProviderSupport {

    public static void setupPactHttpTarget(PactVerificationContext context, int port) {
        if (context == null) {
            return; // If there is no pact, there will be no context.
        }
        context.setTarget(new HttpTestTarget("localhost", port, "/"));
    }
}
