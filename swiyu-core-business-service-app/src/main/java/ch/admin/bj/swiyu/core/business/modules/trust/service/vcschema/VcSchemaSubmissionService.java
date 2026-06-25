package ch.admin.bj.swiyu.core.business.modules.trust.service.vcschema;

import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.exceptions.ObjectCountLimitApiException;
import ch.admin.bj.swiyu.core.business.common.exceptions.PartnerIsNotGovernmentalException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.CreateVcMetadataTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VcSchemaSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcSchemaSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcSchemaSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcTypeMetadataValidator;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.VcSchemaSubmissionNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.EventMapper;
import java.util.Optional;
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
public class VcSchemaSubmissionService {

    private final VcSchemaSubmissionRepository vcSchemaSubmissionRepository;
    private final VcTypeMetadataValidator vcTypeMetadataValidator;
    private final BusinessPartnerService businessPartnerService;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public VcSchemaSubmissionDto createVcSchemaSubmission(
        CreateVcMetadataTypeDto createVcMetadataTypeDto,
        UUID partnerId
    ) throws ObjectCountLimitApiException {
        vcTypeMetadataValidator.validateVcTypeMetadata(createVcMetadataTypeDto.vcTypeMetadata());
        if (!businessPartnerService.isGovernmental(partnerId)) {
            throw new PartnerIsNotGovernmentalException("Business partner is not of type GOVERNMENTAL_INSTITUTION");
        }
        VcSchemaSubmission vcSchemaSubmission = vcSchemaSubmissionRepository.save(
            new VcSchemaSubmission(partnerId, createVcMetadataTypeDto.vcTypeMetadata())
        );

        domainEventPublisher.publishVcSchemaSubmissionAcceptedEvent(
            EventMapper.mapToTiVcSchemaSubmissionAcceptedEvent(vcSchemaSubmission.getId())
        );

        return getVcSchemaSubmissionDto(vcSchemaSubmission);
    }

    @Transactional
    public void markAsSucceeded(UUID id) {
        var vcSchemaSubmission = vcSchemaSubmissionRepository
            .findById(id)
            .orElseThrow(() -> new VcSchemaSubmissionNotFoundException(id.toString(), null));
        vcSchemaSubmission.markAsSucceeded();
    }

    @Transactional
    public void markAsFailed(UUID id, String reason) {
        var vcSchemaSubmission = vcSchemaSubmissionRepository
            .findById(id)
            .orElseThrow(() -> new VcSchemaSubmissionNotFoundException(id.toString(), null));
        vcSchemaSubmission.markAsFailed(reason);
    }

    @Transactional(readOnly = true)
    public Page<VcSchemaSubmissionDto> getAllEntities(UUID partnerId, Pageable pageable) {
        return vcSchemaSubmissionRepository
            .findAllByPartnerId(
                partnerId,
                PageableUtils.toDbPageableFromUserPageable(
                    VcSchemaSubmissionDto.class,
                    VcSchemaSubmission.class,
                    pageable
                )
            )
            .map(this::getVcSchemaSubmissionDto);
    }

    @Transactional(readOnly = true)
    public VcSchemaSubmissionDto getVcSchemaSubmission(UUID id) {
        Optional<VcSchemaSubmissionDto> optional = vcSchemaSubmissionRepository
            .findById(id)
            .map(this::getVcSchemaSubmissionDto);
        if (optional.isEmpty()) {
            throw new ResourceNotFoundException("No VcSchemaSubmission found with id " + id);
        }
        return optional.orElse(null);
    }

    private VcSchemaSubmissionDto getVcSchemaSubmissionDto(VcSchemaSubmission vcSchemaSubmission) {
        return VcSchemaMapper.toVcSchemaSubmissionDto(vcSchemaSubmission);
    }
}
