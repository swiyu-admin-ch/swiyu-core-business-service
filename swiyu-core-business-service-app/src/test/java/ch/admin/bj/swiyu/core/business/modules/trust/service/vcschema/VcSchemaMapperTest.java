package ch.admin.bj.swiyu.core.business.modules.trust.service.vcschema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.admin.bj.swiyu.core.business.modules.trust.api.VcSchemaSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VcSchemaSubmissionStatusDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcSchemaSubmission;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VcSchemaMapperTest {

    @Test
    void testToVcSchemaSubmissionDto() {
        // Given
        var partnerId = UUID.randomUUID();
        VcSchemaSubmission source = new VcSchemaSubmission(partnerId, "schema.json");

        // When
        VcSchemaSubmissionDto dto = VcSchemaMapper.toVcSchemaSubmissionDto(source);

        // Then
        assertEquals(VcSchemaSubmissionStatusDto.ACCEPTED, dto.status());
        assertEquals("schema.json", dto.file());
        assertEquals(partnerId, dto.partnerId());
    }
}
