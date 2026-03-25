package com.segula.saasgestion.domain;

public enum TechnicalOffice {
    FRONT_OFFICE("Front Office"),
    BACK_OFFICE("Back Office");

    private final String dbValue;

    TechnicalOffice(String dbValue) { this.dbValue = dbValue; }

    public String getDbValue() { return dbValue; }

    public static TechnicalOffice fromDbValue(String value) {
        for (TechnicalOffice t : values()) {
            if (t.dbValue.equalsIgnoreCase(value)) return t;
        }
        throw new IllegalArgumentException("Unknown office: " + value);
    }
}