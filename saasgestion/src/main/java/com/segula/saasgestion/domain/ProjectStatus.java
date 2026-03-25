package com.segula.saasgestion.domain;

public enum ProjectStatus {
    ON_GOING("On Going"),
    CLOSED("Closed"),
    PLANNED("Planned"),
    ON_HOLD("On Hold"),
    CANCELED("Canceled");

    private final String dbValue;

    ProjectStatus(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    public static ProjectStatus fromDbValue(String value) {
        for (ProjectStatus s : values()) {
            if (s.dbValue.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}