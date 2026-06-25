package ch.admin.bj.swiyu.core.business.modules.management.domain.pams;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PamsHealthApiClient {

    private final RestClient restClient;

    PamsHealthApiClient(PamsProperties pamsProperties) {
        this.restClient = RestClient.builder().baseUrl(pamsProperties.getApiUrl()).build();
    }

    public PamsHealthResponseDto getHealth() throws RestClientResponseException {
        var response = this.restClient.get()
            .uri("/api/isalive")
            .retrieve()
            .onStatus(
                status -> status.value() != 200,
                (req, res) -> {
                    throw new RestClientResponseException(
                        "Error fetching details from PAMS API health endpoint",
                        res.getStatusCode(),
                        res.getStatusCode().toString(),
                        null,
                        null,
                        null
                    );
                }
            )
            .toEntity(PamsHealthResponseDto.class);

        return response.getBody();
    }
}
