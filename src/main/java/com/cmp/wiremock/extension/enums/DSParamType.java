package com.cmp.wiremock.extension.enums;

/**
 * Created by lunabulnes
 */
public enum DSParamType {

    //Set value params
    BY_XPATH("xpath"),
    BY_NODE_NAME("nodeName"),
    BY_JSON_PATH("jsonPath"),
    BY_JSON_KEY("jsonKey"),

    //Get value params
    FROM_NEW("getNewValue"),
    FROM_RANDOM("getRandomValue"),
    FROM_QUERY_STRING("getFromQueryParameter"),
    FROM_BODY_JSON_PATH("getFromBodyJsonPath"),
    FROM_BODY_JSON_KEY("getFromBodyJsonKey"),
    FROM_BODY_XML("getFromBodyXpath"),
    FROM_BODY_PLAINTEXT("getFromBodyPlainText"),
    FROM_HEADER("getFromHeader"),
    FROM_COOKIE("getFromCookie"),
    COMPOUND_VALUE("compoundValue"),
    FROM_SAVED_RESPONSE("fromResponse"),
    FROM_REQUEST("fromRequest"),
    SAVE_RESPONSE("saveResponseWithTag"),
    DELETE_RESPONSE("deleteResponseWithTag");

    private String paramKey;

    DSParamType(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getKey() {
        return this.paramKey;
    }
}
