package com.cmp.wiremock.extension.enums;

/**
 * Created by lunabulnes
 */
public enum DSParamType {

    POSTBACK_PARAMS("doPostbacks"),
    KEY_PARAMS("key"),
    VALUE_PARAMS("value"),

    //Template type params
    XML_PARAMS("transformXmlNode"),
    JSON_PARAMS("transformJsonNode"),
    PLAIN_TEXT_PARAMS("transformAsPlainText"),

    //Set value params
    BY_XPATH("fillByXpath"),
    BY_NODE_NAME("fillByNodeName"),
    BY_JSON_PATH("fillByJsonPath"),
    BY_JSON_KEY("fillByJsonKey"),
    BY_REGEX("fillByRegex"),
    WITH_URL("withUrl"),
    WITH_METHOD("withMethod"),
    WITH_HEADERS("withHeaders"),
    WITH_COOKIES("withCookies"),
    WITH_BODY("withBody"),

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
    FROM_SAVED_RESPONSE("fromSavedResponse"),
    FROM_REQUEST("fromRequest"),
    SAVE_RESPONSE("saveResponseWithTag"),
    WITH_TAG("withTag"),
    DELETE_RESPONSE("deleteResponseWithTag");

    private String paramKey;

    DSParamType(String paramKey) {
        this.paramKey = paramKey;
    }

    public String getKey() {
        return this.paramKey;
    }
}
