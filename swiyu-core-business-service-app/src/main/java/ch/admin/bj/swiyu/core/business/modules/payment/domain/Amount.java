package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Minor-unit monetary amount (e.g. Rappen for CHF).
 */
@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA
public class Amount {

    @NotNull
    private Long value;

    @NotNull
    private String currency;

    public static Amount chf(long value) {
        return new Amount(value, "CHF");
    }
}
