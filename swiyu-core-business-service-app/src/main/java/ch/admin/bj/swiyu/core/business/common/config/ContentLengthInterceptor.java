/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.core.business.common.config;

import ch.admin.bj.swiyu.core.business.common.exceptions.MaxSizeApiException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.unit.DataSize;

@RequiredArgsConstructor
public class ContentLengthInterceptor implements ClientHttpRequestInterceptor {

    /**
     * The type of resource which is validated.
     * <p>
     * Used in the exception to hint to to the user which resource offended the maximum size restrict.ions
     * <p>
     * Example: "DidLog" if you resolve a DID or "TokenStatusList" if you resolve a StatusList JWT.
     */
    private final String validatingResourceType;
    private final DataSize maxSize;

    @NotNull
    @Override
    public ClientHttpResponse intercept(
        @NotNull HttpRequest request,
        @NotNull byte[] body,
        ClientHttpRequestExecution execution
    ) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        var contentLength = DataSize.ofBytes(response.getHeaders().getContentLength());

        if (this.maxSize.compareTo(contentLength) < 0) {
            throw new MaxSizeApiException(this.maxSize, contentLength, validatingResourceType);
        }

        return response;
    }
}
