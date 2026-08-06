package ch.admin.bj.swiyu.core.business.test;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.DEFAULT_ENTITY;

import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProtectedVerificationCategoryDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProtectedVerificationSubmissionRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationCategory;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.ProtectedVerificationMapper;
import java.util.UUID;

public class ProtectedVerificationSubmissionTestData {

    public static ProtectedVerificationSubmissionRequestDto protectedVerificationSubmissionRequestDto() {
        return protectedVerificationSubmissionRequestDto(DEFAULT_ENTITY);
    }

    public static ProtectedVerificationSubmissionRequestDto protectedVerificationSubmissionRequestDto(UUID partnerId) {
        return ProtectedVerificationSubmissionRequestDto.builder()
            .partnerId(partnerId)
            .sbnId(UUID.randomUUID())
            .entityName("Hello World AG")
            .uid("CHE-123.456.789")
            .contactPerson(
                ContactDto.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .phone("+41 79 123 45 67")
                    .build()
            )
            .category(ProtectedVerificationCategoryDto.PERSONAL_ADMINISTRATIVE_NUMBER)
            .reason("We need to verify the AHV number for compliance purposes.")
            .build();
    }

    public static ProtectedVerificationSubmission protectedVerificationSubmission() {
        return protectedVerificationSubmission(DEFAULT_ENTITY);
    }

    public static ProtectedVerificationSubmission protectedVerificationSubmission(UUID partnerId) {
        var dto = protectedVerificationSubmissionRequestDto(partnerId);
        return new ProtectedVerificationSubmission(
            dto.partnerId(),
            dto.sbnId(),
            dto.entityName(),
            dto.uid(),
            ProtectedVerificationMapper.toContactEntity(dto.contactPerson()),
            dto.reason(),
            ProtectedVerificationCategory.PERSONAL_ADMINISTRATIVE_NUMBER
        );
    }
}
