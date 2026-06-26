package ch.admin.bj.swiyu.core.business.modules.management.domain.pams;

import ch.admin.bj.swiyu.core.business.common.exceptions.ExternalSystem;
import ch.admin.bj.swiyu.core.business.common.exceptions.ExternalSystemException;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.eportal.pams.client.api.BusinessPartnerApi;
import ch.admin.eportal.pams.client.model.ApiBusinesspartnersForeignIDPutRequest;
import ch.admin.eportal.pams.client.model.ApiBusinesspartnersForeignIDPutRequestProfileGroupsInner;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;

@AllArgsConstructor
@Slf4j
class DefaultPamsClient implements PamsClient {

    private final BusinessPartnerApi businessPartnerApi;
    private final PamsProperties pamsProperties;
    private final PamsHealthApiClient pamsHealthApiClient;

    @PostConstruct
    public void postConstruct() {
        log.debug(
            "Created PAMS client with URL {} and appId {}",
            pamsProperties.api().url(),
            pamsProperties.api().appId()
        );
    }

    @Override
    public void createBusinessPartner(BusinessEntity businessPartner, String pamsUserAdminDirUid) {
        try {
            businessPartnerApi.getApiClient().setBearerToken(pamsProperties.createToken());
            var bp = new ch.admin.eportal.pams.client.model.BusinessPartner();
            bp.setForeignID(businessPartner.getId().toString());
            bp.setName(LocalizedMapUtil.getDefaultValue(businessPartner.getEntityName()));

            var bpAutoAssignedProfileGroup = new ApiBusinesspartnersForeignIDPutRequestProfileGroupsInner();
            bpAutoAssignedProfileGroup.setProfileGroupForeignID(pamsProperties.autoAppliedProfileGroupId());
            bpAutoAssignedProfileGroup.setAdminUserUIDs(List.of(pamsUserAdminDirUid));

            bp.setProfileGroups(List.of(bpAutoAssignedProfileGroup));

            var response = businessPartnerApi.apiBusinesspartnersPost(1, bp);
            if (Boolean.FALSE.equals(response.getSuccess())) {
                throw new ExternalSystemException(
                    "Error with system 'PAMS' during creation of BusinessPartner. Details: %s %s".formatted(
                        response.getErrorCode(),
                        response.getMessage()
                    ),
                    ExternalSystem.PAMS,
                    HttpStatusCode.valueOf(response.getStatusCode().intValue())
                );
            }
        } catch (RestClientResponseException e) {
            throw new ExternalSystemException(
                "Error with system 'PAMS' during creation of BusinessPartner.",
                ExternalSystem.PAMS,
                e.getStatusCode()
            );
        }
    }

    @Override
    public void updateBusinessPartner(BusinessEntity businessPartner) {
        try {
            // The PAMS api spec does not specify that this endpoint is protected, but it actually is protected.
            // Due to this inconsistency we cannot rely on openapi bearer token helpers here and need to populate
            // the header ourselves
            businessPartnerApi
                .getApiClient()
                .addDefaultHeader("Authorization", "Bearer %s".formatted(pamsProperties.createToken()));
            var bp = new ApiBusinesspartnersForeignIDPutRequest();
            bp.setName(LocalizedMapUtil.getDefaultValue(businessPartner.getEntityName()));
            var response = businessPartnerApi.apiBusinesspartnersForeignIDPut(
                businessPartner.getId().toString(),
                0,
                bp
            );
            if (Boolean.FALSE.equals(response.getSuccess())) {
                throw new ExternalSystemException(
                    "Error with system 'PAMS' during update of BusinessPartner. Details: %s %s".formatted(
                        response.getErrorCode(),
                        response.getMessage()
                    ),
                    ExternalSystem.PAMS,
                    HttpStatusCode.valueOf(response.getStatusCode().intValue())
                );
            }
        } catch (RestClientResponseException e) {
            throw new ExternalSystemException(
                "Error with system 'PAMS' during update of BusinessPartner.",
                ExternalSystem.PAMS,
                e.getStatusCode()
            );
        }
    }

    @Override
    public void deleteBusinessPartner(String foreignId) {
        try {
            // The PAMS api spec does not specify that this endpoint is protected, but it actually is protected.
            // Due to this inconsistency we cannot rely on openapi bearer token helpers here and need to populate
            // the header ourselves
            businessPartnerApi
                .getApiClient()
                .addDefaultHeader("Authorization", "Bearer %s".formatted(pamsProperties.createToken()));
            var response = businessPartnerApi.apiBusinesspartnersForeignIDDelete(foreignId);
            if (Boolean.FALSE.equals(response.getSuccess())) {
                throw new ExternalSystemException(
                    "Error with system 'PAMS' during deletion of BusinessPartner. Details: %s %s".formatted(
                        response.getErrorCode(),
                        response.getMessage()
                    ),
                    ExternalSystem.PAMS,
                    HttpStatusCode.valueOf(response.getStatusCode().intValue())
                );
            }
        } catch (RestClientResponseException e) {
            throw new ExternalSystemException(
                "Error with system 'PAMS' during deletion of BusinessPartner.",
                ExternalSystem.PAMS,
                e.getStatusCode()
            );
        }
    }

    public void getHealth() {
        try {
            var response = pamsHealthApiClient.getHealth();

            if (Boolean.FALSE.equals(response.success()) || Boolean.FALSE.equals(response.data().isApiRunning())) {
                throw new IllegalStateException(
                    "Error with system 'PAMS' during health call. Please check if the system is available."
                );
            }
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Error with system 'PAMS' during getting of Profile.", e);
        }
    }
}
