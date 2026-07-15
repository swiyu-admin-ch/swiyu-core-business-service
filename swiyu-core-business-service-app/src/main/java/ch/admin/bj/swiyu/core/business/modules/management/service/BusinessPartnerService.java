package ch.admin.bj.swiyu.core.business.modules.management.service;

import static ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType.GOVERNMENTAL_INSTITUTION;
import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerType;
import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerTypeDto;
import static ch.admin.bj.swiyu.core.business.modules.management.service.mapper.BusinessPartnerMapper.toAddress;
import static org.springframework.util.StringUtils.hasText;

import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.ListItemDto;
import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.audit.AuditMapper;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.api.*;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.management.service.mapper.BusinessPartnerMapper;
import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@AllArgsConstructor
@Service
public class BusinessPartnerService {

    private static final String BUSINESS_PARTNER_WITH_ID_S_NOT_FOUND = "Business partner with id '%s' not found.";
    private static final Map<String, String> BUSINESS_PARTNER_SORT_FIELDS = Map.of(
        "name",
        "defaultEntityName",
        "entityName",
        "defaultEntityName"
    );
    private final BusinessPartnerRepository businessPartnerRepository;
    private final PamsClient pamsClient;
    private final IdentifierEntryService identifierEntryService;
    private final StatusListEntryService statusListEntryService;
    private final AuditPublisher auditPublisher;

    @Transactional(readOnly = true)
    public Page<BusinessEntityDto> getAllEntities(List<UUID> businessEntityIds, Pageable pageable) {
        return businessPartnerRepository
            .findAllByIdIn(businessEntityIds, toBusinessPartnerPageable(BusinessEntityDto.class, pageable))
            .map(this::toBusinessEntityDto);
    }

    @Transactional(readOnly = true)
    public Optional<BusinessEntityDto> getBusinessEntity(UUID id) {
        return businessPartnerRepository.findById(id).map(this::toBusinessEntityDto);
    }

    @SuppressWarnings("java:S1874") // allow V1 constructor of BusinessEntity, will be removed
    @Transactional
    public BusinessEntityDto createBusinessPartnerV1(CreateBusinessEntityDto request, String pamsUserAdminDirUid) {
        log.info(
            "Creating new business partner (V1) with name '{}' and contact email '{}'",
            request.name(),
            request.contactEmailAddress()
        );

        if (!hasText(pamsUserAdminDirUid)) {
            throw new IllegalArgumentException("Missing PAMS Admin User UID for creating Business partner");
        }
        var businessPartner = new BusinessEntity(
            UUID.randomUUID(),
            request.name(),
            request.contactEmailAddress(),
            toBusinessPartnerType(request.type())
        );
        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner); // Needs flush for DB Data integrity
        auditPublisher.businessPartnerRegistered(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        pamsClient.createBusinessPartner(businessPartner, pamsUserAdminDirUid);
        return toBusinessEntityDto(businessPartner);
    }

    @Transactional
    public BusinessPartnerDto createBusinessPartnerV2(CreatePartnerDto request, String pamsUserAdminDirUid) {
        log.info(
            "Creating new business partner (V2) with name '{}' and contact email '{}'",
            request.name(),
            request.contactEmail()
        );

        if (!hasText(pamsUserAdminDirUid)) {
            throw new IllegalArgumentException("Missing PAMS Admin User UID for creating Business partner");
        }
        var businessPartner = new BusinessEntity(
            UUID.randomUUID(),
            LocalizedMapUtil.fromSingleName(request.name()),
            request.contactEmail(),
            toBusinessPartnerType(request.partnerType()),
            toAddress(request),
            request.uid(),
            request.contactPhone()
        );
        // this will likely move to its own method once we have payment support. See feature: EIDARTFE-1297
        businessPartner.addPayedForDidSlots(1);
        businessPartner = businessPartnerRepository.save(businessPartner); // Needs flush for DB Data integrity
        identifierEntryService.createIdentifierEntry(businessPartner.getId());
        businessPartnerRepository.flush();
        auditPublisher.businessPartnerRegistered(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        pamsClient.createBusinessPartner(businessPartner, pamsUserAdminDirUid);
        return toBusinessPartnerDto(businessPartner);
    }

    @Transactional
    public BusinessEntityDto updateBusinessEntity(
        UUID businessEntityId,
        UpdateBusinessEntityDto updateBusinessEntityDto
    ) {
        log.info("Updating business partner with id '{}'", businessEntityId);

        BusinessEntity businessPartner = businessPartnerRepository
            .findById(businessEntityId)
            .orElseThrow(throwNotFoundException(businessEntityId));
        businessPartner.update(
            businessPartner.getEntityName(),
            updateBusinessEntityDto.contactEmailAddress(),
            businessPartner.getAddress(),
            businessPartner.getUid(),
            businessPartner.getContactPhone()
        );

        // Only update PAMS if relevant data changed
        var previousDefaultName = LocalizedMapUtil.getDefaultValue(businessPartner.getEntityName());
        var newDefaultName = updateBusinessEntityDto.name();
        if (!previousDefaultName.equals(newDefaultName)) {
            businessPartner.setName(LocalizedMapUtil.fromSingleName(updateBusinessEntityDto.name()));
            pamsClient.updateBusinessPartner(businessPartner);
        }
        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner);
        auditPublisher.businessPartnerUpdated(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        return toBusinessEntityDto(businessPartner);
    }

    @Transactional
    public void updateBusinessPartner(
        UUID businessPartnerId,
        Map<String, String> entityName,
        Address address,
        String email,
        String uid,
        String phone,
        BusinessPartnerType type
    ) {
        log.info("Updating business partner with id '{}' from trust onboarding submission", businessPartnerId);
        BusinessEntity businessPartner = businessPartnerRepository
            .findById(businessPartnerId)
            .orElseThrow(throwNotFoundException(businessPartnerId));

        var previousDefaultName = LocalizedMapUtil.getDefaultValue(businessPartner.getEntityName());
        var newDefaultName = LocalizedMapUtil.getDefaultValue(entityName);
        var nameChanged = !previousDefaultName.equals(newDefaultName);

        businessPartner.update(entityName, email, address, uid, phone);
        businessPartner.setType(type);

        if (nameChanged) {
            pamsClient.updateBusinessPartner(businessPartner);
        }

        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner);
        auditPublisher.businessPartnerUpdated(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
    }

    @SuppressWarnings({ "java:S1874" }) // Remove with EID-6656
    @Transactional
    public BusinessEntityDto updateBusinessEntityIsGovernment(UUID businessEntityId, boolean isGovernment) {
        log.warn("Updating business partner with id '{}' to be set as government.", businessEntityId);

        BusinessEntity businessPartner = businessPartnerRepository
            .findById(businessEntityId)
            .orElseThrow(throwNotFoundException(businessEntityId));
        if (
            (isGovernment && businessPartner.getType() == GOVERNMENTAL_INSTITUTION) ||
            (!isGovernment && businessPartner.getType() != GOVERNMENTAL_INSTITUTION)
        ) {
            return null;
        }

        businessPartner.setType(isGovernment ? GOVERNMENTAL_INSTITUTION : BusinessPartnerType.UNKNOWN);
        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner);
        auditPublisher.businessPartnerUpdated(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        return toBusinessEntityDto(businessPartner);
    }

    @Transactional
    public void deleteBusinessEntity(UUID businessEntityId) {
        log.info("Deleting business partner with id '{}'", businessEntityId);

        businessPartnerRepository.deleteById(businessEntityId);
        pamsClient.deleteBusinessPartner(businessEntityId.toString());
    }

    @Transactional(readOnly = true)
    public boolean isGovernmental(UUID partnerId) {
        if (partnerId == null) {
            return false;
        }
        var partnerType = lookupBusinessPartnerType(partnerId);
        return GOVERNMENTAL_INSTITUTION.equals(partnerType);
    }

    @SuppressWarnings({ "java:S1874" }) // Remove with EID-6656
    @Transactional(readOnly = true)
    public BusinessPartnerTypeDto getBusinessPartnerType(UUID partnerId) {
        if (partnerId == null) {
            return BusinessPartnerTypeDto.UNKNOWN;
        }
        var partnerType = lookupBusinessPartnerType(partnerId);
        return toBusinessPartnerTypeDto(partnerType);
    }

    @Transactional(readOnly = true)
    public Page<BusinessPartnerListItemDto> getAllPartnersById(List<UUID> businessPartnerIds, Pageable pageable) {
        return businessPartnerRepository
            .findAllByIdIn(businessPartnerIds, toBusinessPartnerPageable(BusinessPartnerListItemDto.class, pageable))
            .map(this::getBusinessPartnerListItemDto);
    }

    @Transactional(readOnly = true)
    public Page<BusinessPartnerListItemDto> getAllPartners(Pageable pageable) {
        return businessPartnerRepository
            .findAll(toBusinessPartnerPageable(BusinessPartnerListItemDto.class, pageable))
            .map(this::getBusinessPartnerListItemDto);
    }

    @Transactional(readOnly = true)
    public BusinessPartnerDto getBusinessPartner(UUID id) {
        return businessPartnerRepository
            .findById(id)
            .map(this::toBusinessPartnerDto)
            .orElseThrow(throwNotFoundException(id));
    }

    @Transactional
    public void changeTrustVerificationStatus(
        @NotNull UUID businessPartnerId,
        BusinessPartnerTrustStatusDto businessPartnerTrustStatusDto,
        Instant maxDateForTrustVerificationStatus
    ) {
        BusinessEntity businessPartner = businessPartnerRepository
            .findById(businessPartnerId)
            .orElseThrow(throwNotFoundException(businessPartnerId));
        businessPartner.setTrustVerificationStatus(
            BusinessPartnerMapper.toTrustVerificationStatus(businessPartnerTrustStatusDto),
            maxDateForTrustVerificationStatus
        );
    }

    @SuppressWarnings({ "java:S1874" }) // Remove with EID-6656
    private @NonNull BusinessPartnerType lookupBusinessPartnerType(UUID partnerId) {
        return businessPartnerRepository
            .findById(partnerId)
            .map(BusinessEntity::getType)
            .orElse(BusinessPartnerType.UNKNOWN);
    }

    private BusinessPartnerDto toBusinessPartnerDto(BusinessEntity businessPartner) {
        return BusinessPartnerMapper.toBusinessPartnerDto(
            businessPartner,
            List.of(
                statusListEntryService.getCurrentLimits(businessPartner.getId()),
                identifierEntryService.getCurrentLimits(businessPartner.getId())
            )
        );
    }

    private BusinessEntityDto toBusinessEntityDto(BusinessEntity businessPartner) {
        return BusinessPartnerMapper.toBusinessEntityDto(
            businessPartner,
            List.of(
                statusListEntryService.getCurrentLimits(businessPartner.getId()),
                identifierEntryService.getCurrentLimits(businessPartner.getId())
            )
        );
    }

    private BusinessPartnerListItemDto getBusinessPartnerListItemDto(BusinessEntity businessPartner) {
        return BusinessPartnerMapper.toBusinessPartnerListItemDto(
            businessPartner,
            List.of(
                statusListEntryService.getCurrentLimits(businessPartner.getId()),
                identifierEntryService.getCurrentLimits(businessPartner.getId())
            )
        );
    }

    private Pageable toBusinessPartnerPageable(Class<? extends ListItemDto> dtoClass, Pageable pageable) {
        return PageableUtils.toDbPageableFromUserPageable(
            dtoClass,
            BusinessEntity.class,
            pageable,
            BUSINESS_PARTNER_SORT_FIELDS
        );
    }

    private static @NonNull Supplier<ResourceNotFoundException> throwNotFoundException(UUID id) {
        return () -> new ResourceNotFoundException(String.format(BUSINESS_PARTNER_WITH_ID_S_NOT_FOUND, id));
    }
}
