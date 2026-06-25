package ch.admin.bj.swiyu.core.business.common.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.http.MediaType;

@Converter(autoApply = true)
public class MediaTypeConverter implements AttributeConverter<MediaType, String> {

    @Override
    public String convertToDatabaseColumn(MediaType mediaType) {
        return mediaType != null ? mediaType.toString() : null; // NOSONAR
    }

    @Override
    public MediaType convertToEntityAttribute(String dbData) {
        return dbData != null ? MediaType.parseMediaType(dbData) : null; // NOSONAR
    }
}
