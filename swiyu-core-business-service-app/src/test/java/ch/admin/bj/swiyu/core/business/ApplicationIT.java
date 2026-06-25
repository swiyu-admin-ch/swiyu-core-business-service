package ch.admin.bj.swiyu.core.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import ch.admin.bj.swiyu.antivirus.client.model.ScanResult;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsHealthApiClient;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsHealthResponseDataDto;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsHealthResponseDto;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WithAllTestContainerInitializers
@AutoConfigureMockMvc
class ApplicationIT {

    @MockitoBean
    PamsHealthApiClient pamsHealthApiClient;

    @MockitoBean
    ScanApi scanApi;

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var pamsHealthResponse = new PamsHealthResponseDto(
            true,
            HttpStatusCode.valueOf(200),
            new PamsHealthResponseDataDto(true)
        );
        when(pamsHealthApiClient.getHealth()).thenReturn(pamsHealthResponse);
        when(scanApi.scanGet(any())).thenReturn(List.of(new ScanResult()));
    }

    @Test
    void testContextLoads() throws Exception {
        var response = mockMvc
            .perform(MockMvcRequestBuilders.get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andReturn()
            .getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo(
            "{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}"
        );
    }

    @Test
    void testPamsUpButApiDown() throws Exception {
        var pamsHealthResponse = new PamsHealthResponseDto(
            true,
            HttpStatusCode.valueOf(200),
            new PamsHealthResponseDataDto(false)
        );
        when(pamsHealthApiClient.getHealth()).thenReturn(pamsHealthResponse);

        mockMvc
            .perform(MockMvcRequestBuilders.get("/actuator/health"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void testPamsDown() throws Exception {
        doThrow(new IllegalStateException("Error with system 'PAMS'.")).when(pamsHealthApiClient).getHealth();

        mockMvc
            .perform(MockMvcRequestBuilders.get("/actuator/health"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void testHttpMaxRedirectsSystemPropertyIsSet() {
        var maxRedirects = System.getProperty("http.maxRedirects");
        assertEquals("5", maxRedirects);
    }
}
