package ch.admin.bj.swiyu.core.business.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;

public class RestResponsePage<T> extends PagedModel<T> {

    private static final long serialVersionUID = 3248189030448292002L;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestResponsePage(
        @JsonProperty("content") List<T> content,
        @JsonProperty("number") int number,
        @JsonProperty("size") int size,
        @JsonProperty("totalElements") long totalElements
    ) {
        super(new PageImpl<>(content, PageRequest.of(number, Math.max(size, 1)), totalElements));
    }
}
