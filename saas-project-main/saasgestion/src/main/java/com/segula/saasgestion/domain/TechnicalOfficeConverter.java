package com.segula.saasgestion.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TechnicalOfficeConverter implements AttributeConverter<TechnicalOffice, String> {

    @Override
    public String convertToDatabaseColumn(TechnicalOffice office) {
        return office == null ? null : office.getDbValue();
    }

    @Override
    public TechnicalOffice convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : TechnicalOffice.fromDbValue(dbValue);
    }
}