package com.cmp.wiremock.extension.enums;

/**
 * Created by lunabulnes
 */
public enum DSFakeDataType {

    INTEGER("INTEGER"),
    POS_INTEGER("POSITIVE_INT"),
    NEG_INTEGER("NEGATIVE_INT"),
    FLOAT("FLOAT"),
    POS_FLOAT("POSITIVE_FLO"),
    NEG_FLOAT("NEGATIVE_FLO"),
    STRING("STRING"),
    TIMESTAMP("TIMESTAMP_MILIS"),
    EMAIL("EMAIL"),
    NAME("FIRST_NAME"),
    LAST_NAME("LAST_NAME"),
    FULL_NAME("FULL_NAME"),
    PHONE("PHONE"),
    ADDRESS("ADDRESS");

    private String dataKey;

    DSFakeDataType(String dataKey) {
        this.dataKey = dataKey;
    }

    public String dataKey() {
        return this.dataKey;
    }
}
