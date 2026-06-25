package ch.admin.bj.swiyu.core.business.modules.documents.service;

import static ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocument.createTrustOnboardingSubissionPartnerDocument;
import static ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentMapper.toPartnerDocumentDto;

import ch.admin.bj.swiyu.core.business.common.antivirus.AntivirusClient;
import ch.admin.bj.swiyu.core.business.common.antivirus.AntivirusScanResult;
import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.exceptions.DocumentNotFoundException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ExternalSystemException;
import ch.admin.bj.swiyu.core.business.common.exceptions.InternalStorageException;
import ch.admin.bj.swiyu.core.business.common.exceptions.VirusDetectedException;
import ch.admin.bj.swiyu.core.business.common.s3.S3ClientAdapter;
import ch.admin.bj.swiyu.core.business.common.s3.S3Properties;
import ch.admin.bj.swiyu.core.business.common.utils.FileUtil;
import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentDto;
import ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentTypeDto;
import ch.admin.bj.swiyu.core.business.modules.documents.api.TrustOnboardingSubmissionDocumentDto;
import ch.admin.bj.swiyu.core.business.modules.documents.api.TrustOnboardingSubmissionDocumentListItemDto;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocument;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocumentType;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocumentsRepository;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerDocumentService {

    private final S3ClientAdapter s3ClientAdapter;
    private final PartnerDocumentsRepository partnerDocumentsRepository;
    private final S3Properties s3Properties;
    private final AntivirusClient antivirusClient;
    private final AuditPublisher auditPublisher;

    private static String createObjectStorageKey(
        UUID businessPartnerId,
        String documentGroup,
        UUID parentDocumentReferenceId,
        UUID partnerDocumentId,
        String filename
    ) {
        return "%s/%s/%s/%s-%s".formatted(
            businessPartnerId,
            documentGroup,
            parentDocumentReferenceId,
            partnerDocumentId,
            filename
        );
    }

    /**
     * Uploads a file to S3 service, checks for virus and creates a PartnerDocument if it succeeded.
     */
    @Transactional
    public PartnerDocumentDto createTrustOnboardingSubmissionDocument(
        @NotNull UUID businessPartnerId,
        @NotNull UUID trustOnboardingSubmissionId,
        @NotNull PartnerDocumentTypeDto fileType,
        @NotNull MultipartFile file
    ) throws VirusDetectedException, InternalStorageException, ExternalSystemException {
        var fileTypeMapped = PartnerDocumentMapper.toPartnerDocumentType(fileType);
        var bucketConfig = getBucketConfig(fileTypeMapped);

        var sanitizedFilename = FileUtil.sanitizeFilename(file.getOriginalFilename());

        var newPartnerDocumentId = UUID.randomUUID();
        var newPartnerDocumentObjectStorageKey = createObjectStorageKey(
            businessPartnerId,
            bucketConfig.documentGroup(),
            trustOnboardingSubmissionId,
            newPartnerDocumentId,
            sanitizedFilename
        );
        try {
            s3ClientAdapter.upload(bucketConfig.bucketName(), newPartnerDocumentObjectStorageKey, file.getBytes());
        } catch (IOException e) {
            throw new InternalStorageException("System cannot access temporary file.", e);
        }

        AntivirusScanResult scanResult;
        try {
            scanResult = antivirusClient.scanURl(
                s3ClientAdapter.generatePresignedDownloadUrl(
                    bucketConfig.bucketName(),
                    newPartnerDocumentObjectStorageKey,
                    Duration.ofSeconds(5)
                )
            );

            if (scanResult.virusFound()) {
                throw new VirusDetectedException();
            }
            var partnerDocument = partnerDocumentsRepository.save(
                createTrustOnboardingSubissionPartnerDocument(
                    newPartnerDocumentId,
                    businessPartnerId,
                    fileTypeMapped,
                    sanitizedFilename,
                    MediaType.parseMediaType(Objects.requireNonNull(file.getContentType())),
                    newPartnerDocumentObjectStorageKey,
                    trustOnboardingSubmissionId,
                    scanResult.id(),
                    Instant.now()
                )
            );
            auditPublisher.trustOnboardingDocumentUploaded(
                partnerDocument.getId().toString(),
                String.valueOf(partnerDocument.getVersion()),
                businessPartnerId.toString(),
                partnerDocument.getStorageObjectKey()
            );
            return toPartnerDocumentDto(partnerDocument);
        } catch (Exception e) {
            // catch all as every exception would require we delete the object again.
            s3ClientAdapter.deleteObject(bucketConfig.bucketName(), newPartnerDocumentObjectStorageKey);
            throw e;
        }
    }

    @Timed
    @Transactional
    public void deletePartnerDocument(UUID documentEntityId) {
        delete(documentEntityId);
    }

    @Transactional(readOnly = true)
    public boolean isDocumentBelongingToSubmission(UUID documentId, UUID trustOnboardingSubmissionId) {
        return getDocumentById(documentId).getTrustOnboardingSubmissionId().equals(trustOnboardingSubmissionId);
    }

    @Transactional(readOnly = true)
    public Page<PartnerDocumentDto> findAllByTrustOnboardingSubmissionId(
        UUID trustOnboardingSubmissionId,
        Pageable pageable
    ) {
        return this.partnerDocumentsRepository.findAllByTrustOnboardingSubmissionId(
            trustOnboardingSubmissionId,
            PageableUtils.toDbPageableFromUserPageable(
                TrustOnboardingSubmissionDocumentListItemDto.class,
                PartnerDocument.class,
                pageable
            )
        ).map(PartnerDocumentMapper::toPartnerDocumentDto);
    }

    @Transactional(readOnly = true)
    public TrustOnboardingSubmissionDocumentDto getDocumentForTrustOnboardingSubmission(
        UUID trustOnboardingSubmissionId,
        UUID documentId
    ) {
        var partnerDocument = partnerDocumentsRepository
            .findById(documentId)
            .orElseThrow(() ->
                new DocumentNotFoundException("No document '%s' could be found".formatted(documentId), null)
            );
        if (!partnerDocument.getTrustOnboardingSubmissionId().equals(trustOnboardingSubmissionId)) {
            throw new AuthorizationDeniedException(
                "Document '%s' does not belong to trust onboarding '%s'".formatted(
                    documentId,
                    trustOnboardingSubmissionId
                )
            );
        }

        return PartnerDocumentMapper.toTrustOnboardingSubmissionDocumentDto(
            partnerDocument,
            getDownloadUrl(partnerDocument)
        );
    }

    @Transactional(readOnly = true)
    public List<String> getAllStorageKeysByTrustOnboardingSubmissionId(@NotNull UUID trustOnboardingSubmissionId) {
        return partnerDocumentsRepository
            .findAllByTrustOnboardingSubmissionId(trustOnboardingSubmissionId)
            .stream()
            .map(PartnerDocument::getStorageObjectKey)
            .toList();
    }

    @Transactional(readOnly = true)
    public int countPartnerDocumentsByTrustOnboardingSubmissionId(@NotNull UUID trustOnboardingSubmissionId) {
        return partnerDocumentsRepository.countPartnerDocumentsByTrustOnboardingSubmissionId(
            trustOnboardingSubmissionId
        );
    }

    @Transactional(readOnly = true)
    public int countPartnerDocumentsByPartnerId(@NotNull UUID partnerId) {
        return partnerDocumentsRepository.countPartnerDocumentsByPartnerId(partnerId);
    }

    @Transactional
    public void cleanupTrustOnboardingSubmissionDocuments() {
        // To assert that the lock is held (prevents misconfiguration errors)
        LockAssert.assertLocked();

        var documentIdsToCleanUp =
            partnerDocumentsRepository.findAllIdsBelongingToTrustOnboardingSubmissionsInCleanupStatus();
        for (var documentIdToCleanUp : documentIdsToCleanUp) {
            try {
                delete(documentIdToCleanUp);
                log.info("Did delete document {} during cleanup.", documentIdToCleanUp);
            } catch (Exception e) {
                // swallow error as we need to continue with the process even if one document has an error.
                log.error("Could not delete document {}.", documentIdToCleanUp, e);
            }
        }
    }

    private S3Properties.Bucket getBucketConfig(PartnerDocumentType documentType) {
        return switch (documentType) {
            case
                TRUST_ONBOARDING_OTHER,
                TRUST_ONBOARDING_DECLARATION_OF_INTENT -> s3Properties.trustOnboardingSubmissionDocuments();
        };
    }

    private URL getDownloadUrl(PartnerDocument partnerDocument) {
        var bucketConfig = getBucketConfig(partnerDocument.getType());

        return s3ClientAdapter.generatePresignedDownloadUrl(
            bucketConfig.bucketName(),
            partnerDocument.getStorageObjectKey(),
            bucketConfig.expiryOfPreSignedLink()
        );
    }

    private PartnerDocument getDocumentById(UUID documentId) {
        return partnerDocumentsRepository
            .findById(documentId)
            .orElseThrow(() ->
                new DocumentNotFoundException("No document '%s' could be found".formatted(documentId), null)
            );
    }

    private void delete(UUID documentEntityId) {
        var partnerDocument = getDocumentById(documentEntityId);
        var bucketConfig = getBucketConfig(partnerDocument.getType());
        s3ClientAdapter.deleteObject(bucketConfig.bucketName(), partnerDocument.getStorageObjectKey());
        partnerDocumentsRepository.delete(partnerDocument);
    }
}
