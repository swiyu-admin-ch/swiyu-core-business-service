package ch.admin.bj.swiyu.core.business.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;

public class RestResponsePage<T> extends PagedModel<T> {

    private static final long serialVersionUID = 3248189030448292002L;

    // Some controllers return a plain Page<T> (flat number/size/totalElements fields), others return a
    // PagedModel<T> (nested "page" object) - support both shapes since this helper is shared across both.
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestResponsePage(
        @JsonProperty("content") List<T> content,
        @JsonProperty("page") PageMetadata page,
        @JsonProperty("number") Integer number,
        @JsonProperty("size") Integer size,
        @JsonProperty("totalElements") Long totalElements
    ) {
        super(
            new PageImpl<>(
                content,
                PageRequest.of(
                    page != null ? (int) page.number() : number,
                    Math.max(page != null ? (int) page.size() : size, 1)
                ),
                page != null ? page.totalElements() : totalElements
            )
        );
    }
}
