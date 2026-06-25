package ch.admin.bj.swiyu.registry.identifier;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.identifier-registry")
public record IdentifierRegistryProperties(
    /*
     * The format template to create a data read response for a did:web entry.
     * Format specifiers: {0} -> The entry ID as UUID
     */
    @NotNull @NotEmpty String didWebTemplate,

    /*
     * The format template to create a data read response for a did:tdw entry.
     * Format specifiers: {0} -> The entry ID as UUID
     */
    @NotNull @NotEmpty String didTdwRouteTemplate,
    /*
     * The URL template to use for the public identifier services.
     * Example: https://public.registry.admin.ch/api/vi/did/%s -> https://public.registry.admin.ch/api/vi/did/0000-0000-000000-00000
     */
    @NotEmpty String defaultPublicResolveUrlTemplate,
    /*
     * Additional URL templates which are allowed to be uploaded to this base registry
     */
    @Valid List<@NotEmpty String> additionalPublicResolveUrlTemplates
) {
    /**
     * @return the list of allowed base registry URLs for this environment in the form of URL templates
     */
    public List<String> getPublicResolveUrlTemplates() {
        var result = new ArrayList<String>();
        result.add(defaultPublicResolveUrlTemplate);
        if (additionalPublicResolveUrlTemplates != null) {
            result.addAll(additionalPublicResolveUrlTemplates);
        }
        return result;
    }
}
