package ch.admin.bj.swiyu.core.business.modules.trust.service.mapper;

import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import ch.admin.bj.swiyu.core.business.common.api.LanguageDto;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.modules.trust.api.*;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationCategory;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationSubmissionStatus;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProtectedVerificationMapper {

    public static ProtectedVerificationSubmissionDto toProtectedVerificationSubmissionDto(
        ProtectedVerificationSubmission source
    ) {
        return ProtectedVerificationSubmissionDto.builder()
            .id(source.getId())
            .partnerId(source.getPartnerId())
            .sbnId(source.getSbnId())
            .entityName(source.getEntityName())
            .uid(source.getUid())
            .contactPerson(toContactDto(source.getContactPerson()))
            .category(toCategoryDto(source.getCategory()))
            .reason(source.getReason())
            .status(toStatusDto(source.getStatus()))
            .submittedAt(source.getSubmittedAt())
            .createdAt(source.getAuditMetadata().getCreatedAt())
            .updatedAt(source.getAuditMetadata().getLastModifiedAt())
            .build();
    }

    public static ProtectedVerificationSubmissionListItemDto toProtectedVerificationSubmissionListItemDto(
        ProtectedVerificationSubmission source
    ) {
        return ProtectedVerificationSubmissionListItemDto.builder()
            .id(source.getId())
            .partnerId(source.getPartnerId())
            .entityName(source.getEntityName())
            .category(toCategoryDto(source.getCategory()))
            .status(toStatusDto(source.getStatus()))
            .submittedAt(source.getSubmittedAt())
            .createdAt(source.getAuditMetadata().getCreatedAt())
            .updatedAt(source.getAuditMetadata().getLastModifiedAt())
            .build();
    }

    public static ProtectedVerificationCategoryDto toCategoryDto(ProtectedVerificationCategory source) {
        if (source == null) {
            throw new IllegalArgumentException("Category must not be null");
        }
        return switch (source) {
            case PERSONAL_ADMINISTRATIVE_NUMBER -> ProtectedVerificationCategoryDto.PERSONAL_ADMINISTRATIVE_NUMBER;
        };
    }

    public static ProtectedVerificationCategory toCategory(ProtectedVerificationCategoryDto source) {
        if (source == null) {
            throw new IllegalArgumentException("Category must not be null");
        }
        return switch (source) {
            case PERSONAL_ADMINISTRATIVE_NUMBER -> ProtectedVerificationCategory.PERSONAL_ADMINISTRATIVE_NUMBER;
        };
    }

    public static ProtectedVerificationSubmissionStatusDto toStatusDto(ProtectedVerificationSubmissionStatus source) {
        if (source == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
        return switch (source) {
            case SUBMITTED -> ProtectedVerificationSubmissionStatusDto.SUBMITTED;
            case APPROVED -> ProtectedVerificationSubmissionStatusDto.APPROVED;
            case REJECTED -> ProtectedVerificationSubmissionStatusDto.REJECTED;
        };
    }

    public static ContactDto toContactDto(Contact contact) {
        if (contact == null) {
            return null;
        }
        return new ContactDto(
            contact.getFirstName(),
            contact.getLastName(),
            contact.getEmail(),
            contact.getPhone(),
            toLanguageDto(contact.getCorrespondingLanguage()),
            null // deprecated field — not stored
        );
    }

    public static Contact toContactEntity(ContactDto dto) {
        if (dto == null) {
            return null;
        }
        return Contact.builder()
            .firstName(dto.firstName())
            .lastName(dto.lastName())
            .email(dto.email())
            .phone(dto.phone())
            .correspondingLanguage(toLanguage(dto.correspondingLanguage()))
            .build();
        // note: deprecated dto.address() is intentionally not mapped
    }

    private static LanguageDto toLanguageDto(Language source) {
        if (source == null) {
            return null;
        }
        return LanguageDto.valueOf(source.name());
    }

    private static Language toLanguage(LanguageDto source) {
        if (source == null) {
            return null;
        }
        return Language.valueOf(source.name());
    }
}
