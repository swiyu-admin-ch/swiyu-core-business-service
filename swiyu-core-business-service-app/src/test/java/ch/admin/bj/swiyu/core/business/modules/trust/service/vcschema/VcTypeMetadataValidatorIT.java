package ch.admin.bj.swiyu.core.business.modules.trust.service.vcschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.config.JsonSchemaConfig;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustRegistryProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.config.VcTypeMetadataSchemaConfig;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcTypeMetadataValidator;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.VcTypeMetadataValidationFailedException;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.core.business.test.VCTypeMetadataTestData;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.net.MalformedURLException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@DataJpaTest
@WithAllTestContainerInitializers
@WithJeapAuthenticationToken(username = "Test")
@Import(
    {
        DataJpaTestConfiguration.class,
        VcTypeMetadataValidator.class,
        JsonSchemaConfig.class,
        VcTypeMetadataSchemaConfig.class,
        VCTypeMetadataTestData.class,
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VcTypeMetadataValidatorIT {

    @MockitoBean
    private TrustRegistryProperties trustRegistryProperties;

    @Autowired
    private VcTypeMetadataValidator vcTypeMetadataValidator;

    @Autowired
    private VCTypeMetadataTestData testData;

    @BeforeEach
    void setUp() throws MalformedURLException {
        when(trustRegistryProperties.dataServiceBaseUrl()).thenReturn(URI.create("https://test-url.ch").toURL());
    }

    @Test
    public void testValidateVcTypeMetadata_ValidMetadata() throws Exception {
        // Given
        String vcTypeMetadata = testData.validTypeMetadata();

        // Then
        vcTypeMetadataValidator.validateVcTypeMetadata(vcTypeMetadata);
    }

    @Test
    public void testValidateVcTypeMetadata_InvalidMetadata() throws Exception {
        // Given
        String vcTypeMetadata = testData.invalidTypeMetadata();

        // Then
        VcTypeMetadataValidationFailedException exception = assertThrows(
            VcTypeMetadataValidationFailedException.class,
            () -> vcTypeMetadataValidator.validateVcTypeMetadata(vcTypeMetadata)
        );
        assertEquals("Provided VcMetadataType resource is invalid.", exception.getMessage());
    }

    @Test
    public void testValidateVcTypeMetadata_InvalidMetadata_WrongVCT() throws Exception {
        // Given
        String vcTypeMetadata = testData.wrongVcMetadata();

        // Then
        VcTypeMetadataValidationFailedException exception = assertThrows(
            VcTypeMetadataValidationFailedException.class,
            () -> vcTypeMetadataValidator.validateVcTypeMetadata(vcTypeMetadata)
        );
        assertEquals("Provided VcMetadataType resource is invalid.", exception.getMessage());
    }

    @Test
    public void testValidateVcTypeMetadata_NullMetadata() {
        // Then
        VcTypeMetadataValidationFailedException exception = assertThrows(
            VcTypeMetadataValidationFailedException.class,
            () -> vcTypeMetadataValidator.validateVcTypeMetadata(null)
        );
        assertEquals("Provided VcMetadataType resource is invalid.", exception.getMessage());
    }

    @Test
    public void testValidateVcTypeMetadata_EmptyMetadata() {
        // Then
        VcTypeMetadataValidationFailedException exception = assertThrows(
            VcTypeMetadataValidationFailedException.class,
            () -> vcTypeMetadataValidator.validateVcTypeMetadata("")
        );
        assertEquals("Provided VcMetadataType resource is invalid.", exception.getMessage());
    }
}
