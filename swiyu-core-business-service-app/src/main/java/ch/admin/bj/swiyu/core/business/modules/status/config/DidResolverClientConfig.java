/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.core.business.modules.status.config;

import ch.admin.bj.swiyu.core.business.common.config.ContentLengthInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@AllArgsConstructor
public class DidResolverClientConfig {

    private final DidResolverProperties didResolverProperties;

    @Bean
    public RestClient didResolverClient(RestClient.Builder builder) {
        return builder
            .requestInterceptor(new ContentLengthInterceptor("DID Log", didResolverProperties.maxDidLogSize()))
            .build();
    }
}
