package fu.de190270.post.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

/** Preserve a calendar date when the JVM and JSON time zones differ. */
@Converter
public class DateOnlyConverter implements AttributeConverter<Date, LocalDate> {
    @Override
    public LocalDate convertToDatabaseColumn(Date value) {
        return value == null ? null : Instant.ofEpochMilli(value.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
    }

    @Override
    public Date convertToEntityAttribute(LocalDate value) {
        return value == null ? null : Date.from(value.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
