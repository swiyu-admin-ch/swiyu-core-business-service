package ch.admin.bj.swiyu.core.business.common.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Embeddable to provide translated strings.
 */
@Embeddable
@Getter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA
public class MultiLanguageText {

    private String de;
    private String fr;
    private String it;
    private String en;
    private String rm;

    public String getLanguage(Language language) {
        return switch (language) {
            case DE -> de;
            case FR -> fr;
            case IT -> it;
            case EN -> en;
            case RM -> rm;
        };
    }
}
