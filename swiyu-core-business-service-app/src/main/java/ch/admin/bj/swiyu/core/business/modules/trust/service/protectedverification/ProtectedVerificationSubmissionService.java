package ch.admin.bj.swiyu.core.business.modules.trust.service.protectedverification;

import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.email.EmailCommandPublisher;
import ch.admin.bj.swiyu.core.business.common.exceptions.PartnerIsNotTrustedException;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProtectedVerificationCategoryDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProtectedVerificationSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProtectedVerificationSubmissionFilterDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProtectedVerificationSubmissionListItemDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProtectedVerificationSubmissionRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationCategory;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationSubmissionDomainService;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.QProtectedVerificationSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.EventMapper;
import ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.ProtectedVerificationMapper;
import com.querydsl.core.BooleanBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@AllArgsConstructor
@Service
public class ProtectedVerificationSubmissionService {

    private final ProtectedVerificationSubmissionRepository protectedVerificationSubmissionRepository;
    private final ProtectedVerificationSubmissionDomainService protectedVerificationSubmissionDomainService;
    private final BusinessPartnerService businessPartnerService;
    private final DomainEventPublisher domainEventPublisher;
    private final EmailCommandPublisher emailCommandPublisher;

    @Transactional
    public ProtectedVerificationSubmissionDto createProtectedVerificationSubmission(
        ProtectedVerificationSubmissionRequestDto dto
    ) {
        if (!businessPartnerService.isTrusted(dto.partnerId())) {
            throw new PartnerIsNotTrustedException(
                "Business partner '%s' is not trusted, protected verification cannot be requested".formatted(
                    dto.partnerId()
                )
            );
        }

        var protectedVerificationSubmission = protectedVerificationSubmissionRepository.save(
            new ProtectedVerificationSubmission(
                dto.partnerId(),
                dto.sbnId(),
                dto.entityName(),
                dto.uid(),
                ProtectedVerificationMapper.toContactEntity(dto.contactPerson()),
                dto.reason(),
                ProtectedVerificationMapper.toCategory(dto.category())
            )
        );

        domainEventPublisher.publishProtectedVerificationSubmissionAcceptedEvent(
            EventMapper.mapToTiProtectedVerificationSubmissionAcceptedEvent(protectedVerificationSubmission.getId())
        );
        emailCommandPublisher.submissionAccepted(dto.partnerId());

        return ProtectedVerificationMapper.toProtectedVerificationSubmissionDto(protectedVerificationSubmission);
    }

    public List<ProtectedVerificationCategoryDto> getProtectedVerificationCategories() {
        return Arrays.stream(ProtectedVerificationCategory.values())
            .map(ProtectedVerificationMapper::toCategoryDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProtectedVerificationSubmissionDto getProtectedVerificationSubmission(UUID id) {
        return ProtectedVerificationMapper.toProtectedVerificationSubmissionDto(
            protectedVerificationSubmissionDomainService.getProtectedVerificationSubmission(id)
        );
    }

    @Transactional(readOnly = true)
    public Page<ProtectedVerificationSubmissionListItemDto> getAllProtectedVerificationSubmissions(
        ProtectedVerificationSubmissionFilterDto filter,
        Pageable pageable
    ) {
        var q = QProtectedVerificationSubmission.protectedVerificationSubmission;
        var where = new BooleanBuilder();
        if (filter.businessPartnerIds() != null) {
            where.and(q.partnerId.in(filter.businessPartnerIds()));
        }

        return protectedVerificationSubmissionRepository
            .findAll(
                where,
                PageableUtils.toDbPageableFromUserPageable(
                    ProtectedVerificationSubmissionDto.class,
                    ProtectedVerificationSubmission.class,
                    pageable
                )
            )
            .map(ProtectedVerificationMapper::toProtectedVerificationSubmissionListItemDto);
    }

    @Transactional
    public void markAsApproved(UUID id) {
        var protectedVerificationSubmission =
            protectedVerificationSubmissionDomainService.getProtectedVerificationSubmission(id);
        protectedVerificationSubmission.markAsApproved();

        if (isAhv(protectedVerificationSubmission.getCategory())) {
            var partnerId = protectedVerificationSubmission.getPartnerId();
            emailCommandPublisher.trustProtectedVerificationAhvApproved(partnerId);
        }
    }

    @Transactional
    public void markAsRejected(UUID id, String rejectReason) {
        var protectedVerificationSubmission =
            protectedVerificationSubmissionDomainService.getProtectedVerificationSubmission(id);
        protectedVerificationSubmission.markAsRejected(rejectReason);

        if (isAhv(protectedVerificationSubmission.getCategory())) {
            var partnerId = protectedVerificationSubmission.getPartnerId();
            emailCommandPublisher.trustProtectedVerificationAhvRejected(partnerId);
        }
    }

    /**
     * The AHV number is the personal administrative number, which is the only protected
     * verification category to date.
     */
    private static boolean isAhv(ProtectedVerificationCategory category) {
        return category == ProtectedVerificationCategory.PERSONAL_ADMINISTRATIVE_NUMBER;
    }
}
