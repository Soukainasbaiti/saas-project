package com.segula.saasgestion.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProjectStatusConverter implements AttributeConverter<ProjectStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProjectStatus status) {
        return status == null ? null : status.getDbValue();
    }

    @Override
    public ProjectStatus convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : ProjectStatus.fromDbValue(dbValue);
    }
}