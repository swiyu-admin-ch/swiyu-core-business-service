package ch.admin.bj.swiyu.core.business.modules.trust.config;

import static org.junit.jupiter.api.Assertions.*;

import ch.admin.bj.swiyu.discrete.validator.DefaultDiscreteValidatorClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class DefaultDiscreteValidatorClientTest {

    /**
     * Simple test to verify all dependencies are present and the client can be created.
     */
    @Test
    void canCreate() {
        // given/when
        var client = new DefaultDiscreteValidatorClient(
            new ObjectMapper(),
            "http://dummy-url:8080",
            "dummy-username",
            "dummy-password"
        );
        // then
        assertNotNull(client);
    }
}
