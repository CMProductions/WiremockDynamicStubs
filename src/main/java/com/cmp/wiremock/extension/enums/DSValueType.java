package com.cmp.wiremock.extension.enums;

/**
 * Created by lunabulnes
 */
public enum DSValueType {

    NEW("newValue"),
    RANDOM("randomValue"),
    FROM_QUERY_STRING("valueFromQueryParameter"),
    FROM_BODY_JSON("valueFromBodyJsonPath"),
    FROM_BODY_JSONARRAY("valueFromBodyJsonArray"),
    FROM_BODY_XML("valueFromBodyXpath"),
    FROM_BODY_PLAINTEXT("valueFromBodyPlainText"),
    FROM_STATUS_CODE("valueFromStatusCode"),
    FROM_HEADER("valueFromHeader");

    private String paramKey;

    DSValueType(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getKey() {
        return this.paramKey;
    }
}
